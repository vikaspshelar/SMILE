/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.messages;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 7 6 5 4 3 2 1
0 0 0 0 0 0 1
0 0 0 1 0 0 0
0 0 0 1 0 1 0
0 0 0 1 0 1 1
0 0 1 0 1 0 1
0 0 1 1 0 1 1
0 0 1 1 1 0 0
0 0 1 1 1 0 1
0 0 1 1 1 1 0
0 1 0 0 1 1 0
0 1 0 1 0 0 1
0 1 0 1 0 1 0
0 1 0 1 1 1 1
0 1 1 0 0 1 0
1 0 0 0 1 0 1
1 0 1 0 0 0 1
1 0 1 1 1 1 1
1 1 0 0 0 0 0
1 1 0 0 0 0 1
1 1 0 0 0 1 0 #
1
8
10
11
21
27
28
29
30
38
41
42
47
50
69
81
95
96
97
98
1 1 0 0 0 1 1
1 1 0 1 1 1 1
1 1 1 1 1 1 1 99
111
127
Cause
Unassigned (unallocated) number
Operator determined barring
Call barred
Reserved
Short message transfer rejected
Destination out of order
Unidentified subscriber
Facility rejected
Unknown subscriber
Network out of order
Temporary failure
Congestion
Resources unavailable, unspecified
Requested facility not subscribed
Requested facility not implemented
Invalid short message transfer reference value
Semantically incorrect message
Invalid mandatory information
Message type non-existent or not implemented
Message not compatible with short message protocol
state
Information element non-existent or not implemented
Protocol error, unspecified
Interworking, unspecified
All other cause values shall be treated as cause number 41, "Temporary Failure".
Table 8.4/3GPP TS 24.011 (part 2): Cause values that may be contained in an RP-ERROR message in
a mobile terminating SM-transfer attempt
Cause value
Class value
7 6 5 4 3 2 1
0 0 1 0 1 1 0
1 0 1 0 0 0 1
1 0 1 1 1 1 1
1 1 0 0 0 0 0
1 1 0 0 0 0 1
1 1 0 0 0 1 0
1 1 0 0 0 1 1
1 1 0 1 1 1 1
Cause
number
#
22
81
95
96
97
98
99
111
Cause
Memory capacity exceeded
Invalid short message transfer reference value
Semantically incorrect message
Invalid mandatory information
Message type non-existent or not implemented
Message not compatible with short message protocol state
Information element non-existent or not implemented
Protocol error, unspecified
All other cause values shall be treated as cause number 111, "Protocol error, unspecified".
3GPPRelease 1999
37
3GPP TS 24.011 V3.6.0 (2001-03)
Table 8.4/3GPP TS 24.011 (part 3): Cause values that may be contained in an RP-ERROR message in
a memory available notification attempt
Cause value
Class value
7 6 5 4 3 2 1
0 0 1 1 1 1 0
0 1 0 0 1 1 0
0 1 0 1 0 0 1
0 1 0 1 0 1 0
0 1 0 1 1 1 1
1 0 0 0 1 0 1
1 0 1 1 1 1 1
1 1 0 0 0 0 0
1 1 0 0 0 0 1
1 1 0 0 0 1 0
1 1 0 0 0 1 1
1 1 0 1 1 1 1
1 1 1 1 1 1 1
Cause
number
#
30
38
41
42
47
69
95
96
97
98
99
111
127
Cause
type
P
T
T
T
T
P
P
P
P
P
P
P
P
Cause
Unknown Subscriber
Network out of order
Temporary failure
Congestion
Resources unavailable, unspecified
Requested facility not implemented
Semantically incorrect message
Invalid mandatory information
Message type non-existent or not implemented
Message not compatible with short message protocol state
Information element non-existent or not implemented
Protocol error, unspecified
Interworking, unspecified
All other cause values are treated as cause number 41, "Temporary failure".
Each cause is classified as "Temporary" or "Permanent", as indicated by T and P respectively in the cause type
column.
 */

/**
 *
 * @author jaybeepee
 */
public class RpErrorMessage extends RpMessage {
    private static final Logger log = LoggerFactory.getLogger(RpErrorMessage.class);
    private int RpCause = 10;   /* default call-barred */
    
    @Override
    public void deSerialise() {
        throw new UnsupportedOperationException("Incoming RP-ERROR messages not yet supprted for deserialisation"); 
    }
    
    public byte[] serialise() {
        int i = 0;
        List<Byte> byteArray = new ArrayList<Byte>();
        
        byteArray.add(new Byte((byte)5));
        byteArray.add(new Byte((byte)messageReference));
        byteArray.add(new Byte((byte)1)); /* length */
        byteArray.add(new Byte((byte)10));
                
        byte[] retBytes = new byte[byteArray.size()];
        log.debug("MMSIP: printing RpErrorMessage bytes");
        for (i=0 ; i < byteArray.size(); i++) {
            retBytes[i] = byteArray.get(i);
        }

        return retBytes;
    }
    
    
}
