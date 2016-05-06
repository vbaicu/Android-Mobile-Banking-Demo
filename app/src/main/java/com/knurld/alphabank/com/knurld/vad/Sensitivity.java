/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.vad;

public enum Sensitivity {
	
	/*
	 * Sensitivity preset defined for the Endpoint Detection API
	 */
	very_low		(0.3),
	low 			(0.2),
	normal 			(0.1),
	high			(0.05),
	very_high		(0.01);
	
	private final double threshold;
	
	Sensitivity (double threshold) {
		this.threshold = threshold;
	}
	
	public double threshold() {
		return this.threshold;
	}
	
	public Sensitivity lowerSensitivity () {
		switch (this) {
		case very_low:
			return very_low;
		case low:
			return very_low;
		case normal:
			return low;
		case high:
			return normal;
		case very_high:
			return high;
		default:
			return normal;
		}
	}
	
	public Sensitivity increaseSensitivity () {
		switch (this) {
		case very_low:
			return low;
		case low:
			return normal;
		case normal:
			return high;
		case high:
			return very_high;
		case very_high:
			return very_high;
		default:
			return normal;
		}
	}
}
