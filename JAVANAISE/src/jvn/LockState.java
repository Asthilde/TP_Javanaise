package jvn;

import java.io.Serializable;

public enum LockState implements Serializable{
	NL,
	RC,
	WC,
	R,
	W,
	RWC,
}
