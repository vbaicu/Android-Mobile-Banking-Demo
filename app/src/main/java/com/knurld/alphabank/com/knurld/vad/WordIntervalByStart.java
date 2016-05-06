/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.vad;

import java.util.Comparator;

public class WordIntervalByStart implements Comparator<WordIntervalByStart>, Comparable<WordIntervalByStart>{
	
	private int start; //ms
	private int stop; //ms
	
	public WordIntervalByStart(int start, int stop) {
		this.start = start;
		this.stop = stop;
	}
	
	public int getStartTime() {
		return start;
	}
	
	public int getStopTime() {
		return stop;
	}
	
	@Override
	public String toString() {
		return String.format("Start: %d ms - Stop: %d ms", start, stop);
	}

	@Override
	public int compare(WordIntervalByStart o1, WordIntervalByStart o2) {
//		//Descending
//		return o2.getDuration() - o1.getDuration();
		//Ascending
		return o1.getStartTime() - o2.getStartTime();
	}

	@Override
	public int compareTo(WordIntervalByStart o) {
//		//Descending
//		return o.getDuration() - this.getDuration();
		//Ascending
		return this.getStartTime() - o.getStartTime();
	}
}
