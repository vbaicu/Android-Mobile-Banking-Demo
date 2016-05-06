/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.vad;

import java.util.Comparator;

public class WordInterval implements Comparator<WordInterval>, Comparable<WordInterval>{
	
	private int start; //ms
	private int stop; //ms
	
	public WordInterval(int start, int stop) {
		this.start = start;
		this.stop = stop;
	}
	
	public int getStartTime() {
		return start;
	}
	
	public int getStopTime() {
		return stop;
	}
	
	public int getDuration() {
		return stop - start;
	}
	
	@Override
	public String toString() {
		return String.format("Start: %d ms - Stop: %d ms", start, stop);
	}

	@Override
	public int compare(WordInterval o1, WordInterval o2) {
		//Descending
		return o2.getDuration() - o1.getDuration();
	}

	@Override
	public int compareTo(WordInterval o) {
		//Descending
		return o.getDuration() - this.getDuration();
	}
}
