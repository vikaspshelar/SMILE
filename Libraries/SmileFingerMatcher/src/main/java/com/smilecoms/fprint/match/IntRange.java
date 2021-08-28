// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.smilecoms.fprint.match;

class IntRange {
	static final IntRange ZERO = new IntRange(0, 0);
	final int start;
	final int end;
	IntRange(int start, int end) {
		this.start = start;
		this.end = end;
	}
	int length() {
		return end - start;
	}
}
