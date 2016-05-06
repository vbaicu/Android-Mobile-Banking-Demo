/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.vad;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WordDetection {

    //Versioning info
    public static String VERSION = "1.0.3";

    //Run time parameters
    public static long maxBreakTimeBetweenSample = 300; //ms
    public static int bufferSize = 128;
    public static long startTimeToIgnore = 500; //ms
    public static int sensitivityTolerance = 1; //How far should the sensitivity level be adjusted based on the size of WordIntervals

    public static List<WordInterval> detectWords(String wavFileLocation, Sensitivity sensitivity) {
        List<WordInterval> listOfWordInterval = new ArrayList<WordInterval>();
        try {
            // Open the wav file specified as the first argument
            WavFile wavFile = WavFile.openWavFile(new File(wavFileLocation));

            //Display information from the wav file for debugging
//			// Display information about the wav file
//			wavFile.display();

            // Get the number of audio channels in the wav file
            int numChannels = wavFile.getNumChannels();

            // Create a buffer of x frames
            double[] buffer = new double[bufferSize * numChannels];

            int framesRead;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            double avg = 0.0;
            long nSample = 0;

            //Get the average to normalize in case the background is noisy
            do {
                // Read frames into buffer
                framesRead = wavFile.readFrames(buffer, bufferSize);

                // Loop through frames and look for minimum and maximum value
                for (int s = 0; s < framesRead * numChannels; s++) {
                    if (buffer[s] > max) max = buffer[s];
                    if (buffer[s] < min) min = buffer[s];
                    nSample++;
                    avg = (avg * (nSample - 1) + Math.abs(buffer[s])) / nSample;
                }
            }
            while (framesRead != 0);

            // Close the wavFile
            wavFile.close();

            //Now open the new wav file and process
            //Normalize everything by the avg
            wavFile = WavFile.openWavFile(new File(wavFileLocation));
            long sampleRate = wavFile.getSampleRate();
            long totalFramesRead = 0;

            //State variables to detect the start and stop of words
            boolean isRecording = false;
            long skippedFrames = 0;

            //Memory or state to find and save
            float startInterval = 0;
            float stopInterval = 0;
            do {
                // Read frames into buffer
                framesRead = wavFile.readFrames(buffer, bufferSize);
                totalFramesRead += bufferSize;

                //Check to see if this is more than the start interval to ignore at the beginning
                if ((float) totalFramesRead / (float) sampleRate * 1000 > startTimeToIgnore) {

                    // Loop through frames and look for minimum and maximum value
                    for (int s = 0; s < framesRead * numChannels; s++) {
                        //Mark the start of the recording interval
                        double normalizedReading = Math.abs(buffer[s]) - avg;
                        if (normalizedReading > sensitivity.threshold() && !isRecording) {
                            isRecording = true;
                            startInterval = (float) totalFramesRead / (float) sampleRate * 1000;
                        } else if (normalizedReading < sensitivity.threshold() && isRecording) {
                            skippedFrames++;
                            double breakTimeSoFar = 1000 * (double) skippedFrames / (double) sampleRate;
                            if (breakTimeSoFar > maxBreakTimeBetweenSample) {
                                //Stop recording and reset everything
                                isRecording = false;
                                skippedFrames = 0;
                                stopInterval = (float) totalFramesRead / (float) sampleRate * 1000;
                            }
                        } else if (normalizedReading > sensitivity.threshold() && isRecording) {
                            //Reset skipped frames since we are still in the recording region
                            skippedFrames = 0;
                        }

                        //Save state if both start and stop are found
                        if (startInterval > 0 && stopInterval > 0) {

                            //Add to the list
                            listOfWordInterval.add(new WordInterval(Math.round(startInterval), Math.round(stopInterval)));

                            //Reset start and stop for the next cycle
                            startInterval = 0;
                            stopInterval = 0;
                        }
                    }
                }
            }
            while (framesRead != 0);
        } catch (Exception e) {
            System.err.println(e);
        }
        return listOfWordInterval;
    }

    public static List<WordInterval> selectRepeatWords(List<WordInterval> rawWordIntervalList, int repeats) {
        /*
         * Sort then select the words with the longest durations
		 * Resorted 
		 */
        List<WordInterval> finalWordIntervalList = new ArrayList<WordInterval>();
        if (rawWordIntervalList.size() < repeats) {
            return finalWordIntervalList;
        } else if (rawWordIntervalList.size() > repeats) {
            //More than enough, most likely there are some false positives in the voice recording
            //Loop through and select the 3 longest intervals
            Collections.sort(rawWordIntervalList);
            for (int i = 0; i < repeats; i++) {
                finalWordIntervalList.add(rawWordIntervalList.get(i));
            }
        } else {
            //Equal, just pass through, do not process anything
            finalWordIntervalList.addAll(rawWordIntervalList);
        }

        //Cleanup
        rawWordIntervalList.clear();
        rawWordIntervalList = null;
        sort(finalWordIntervalList);
        return finalWordIntervalList;
    }


    public static void printWordIntervalList(List<WordInterval> wordIntervalList) {
        for (WordInterval wi : wordIntervalList) {
            System.out.println(wi.toString());
        }
    }

    private static void sort(List<WordInterval> finalWordIntervalList) {
        Collections.sort(finalWordIntervalList, new Comparator<WordInterval>() {
            @Override
            public int compare(WordInterval o1, WordInterval o2) {
                return o1.getStartTime() - o2.getStartTime();
            }
        });
    }


    public static List<WordInterval> detectWordsAutoSensitivity(String wavFileLocation, int repeats) {
        /*
         * Automatically detect words by determining how far it's from the expected number of words
		 */
        Sensitivity selectedSensitivity = Sensitivity.normal;
        List<WordInterval> detectedResults = detectWords(wavFileLocation, selectedSensitivity);
        List<WordInterval> _detectedResults = new ArrayList<>(detectedResults);

        //Return if the current result is within the tolerance limit
        if (Math.abs(detectedResults.size() - repeats) <= sensitivityTolerance) {
            //Don't do anything

        } else if (detectedResults.size() < repeats) {
            //Need to increase sensitivity
            while (detectedResults.size() < repeats && selectedSensitivity != Sensitivity.very_high) {
                selectedSensitivity = selectedSensitivity.increaseSensitivity();
                detectedResults = detectWords(wavFileLocation, selectedSensitivity);
            }

        } else {
            //Need to lower sensitivity
            while (detectedResults.size() > repeats && selectedSensitivity != Sensitivity.very_low) {
                selectedSensitivity = selectedSensitivity.lowerSensitivity();
                detectedResults = detectWords(wavFileLocation, selectedSensitivity);
            }

        }

        //Print out or send metrics about selected sensitivity
        System.out.println("Selected Sensitivity: " + selectedSensitivity);

        if (repeats > detectedResults.size()) {
            return selectRepeatWords(_detectedResults, repeats);
        }
        return selectRepeatWords(detectedResults, repeats);
    }

    public static List<WordInterval> resortBasedOnStart(List<WordInterval> wordIntervals) {
        /*
         * Take in a word interval then resort based on the start time
		 */
        List<WordInterval> newWIL = new ArrayList<>();
        List<WordIntervalByStart> temp = new ArrayList<>();
        for (WordInterval wordInterval : wordIntervals) {
            temp.add(new WordIntervalByStart(wordInterval.getStartTime(), wordInterval.getStopTime()));
        }
        for (WordIntervalByStart wibs : temp) {
            newWIL.add(new WordInterval(wibs.getStartTime(), wibs.getStopTime()));
        }
//        wordIntervals.forEach(wi -> temp.add(new WordIntervalByStart(wi.getStartTime(), wi.getStopTime())));
//        Collections.sort(temp);
//        temp.forEach(wibs -> newWIL.add(new WordInterval(wibs.getStartTime(), wibs.getStopTime())));
        return newWIL;
    }
}
