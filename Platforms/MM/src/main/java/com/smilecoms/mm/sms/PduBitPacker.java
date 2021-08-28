
package com.smilecoms.mm.sms;

/**
 *
 * @author jaybeepee
 */
public class PduBitPacker
    {
        private static final byte[] _encodeMask = new byte[] { 1, 3, 7, 15, 31, 63, 127 };
        private static byte[] _decodeMask = new byte[] { (byte)128, (byte)192, (byte)224, (byte)240, (byte)248, (byte)252, (byte)254 };

        /// <summary>
        /// Packs an unpacked 7 bit array to an 8 bit packed array according to the GSM
        /// protocol.
        /// </summary>
        /// <param name="unpackedBytes">The byte array that should be packed.</param>
        /// <returns>The packed bytes array.</returns>
        public static byte[] PackBytes(byte[] unpackedBytes, int hasUserDataHeader) throws Exception
        {
            return PackBytes(unpackedBytes, false, ' ', hasUserDataHeader);
        }

        /// <summary>
        /// Packs an unpacked 7 bit array to an 8 bit packed array according to the GSM
        /// protocol.
        /// </summary>
        /// <param name="unpackedBytes">The byte array that should be packed.</param>
        /// <param name="replaceInvalidChars">Indicates if invalid characters should be replaced by a '?' character.</param>
        /// <returns>The packed bytes array.</returns>
        public static byte[] PackBytes(byte[] unpackedBytes, boolean replaceInvalidChars, int hasUserDataHeader) throws Exception
        {
            return PackBytes(unpackedBytes, replaceInvalidChars, '?', hasUserDataHeader);
        }

        /// <summary>
        /// Packs an unpacked 7 bit array to an 8 bit packed array according to the GSM
        /// protocol.
        /// </summary>
        /// <param name="unpackedBytes">The byte array that should be packed.</param>
        /// <param name="replaceInvalidChars">Indicates if invalid characters should be replaced by the default character.</param>
        /// <param name="defaultChar">The character that replaces invalid characters.</param>
        /// <returns>The packed bytes array.</returns>
        public static byte[] PackBytes(byte[] unpackedBytes, boolean replaceInvalidChars, char defaultChar, int hasUserDataHeader) throws Exception
        {
            byte defaultByte = (byte)defaultChar;
            byte[] shiftedBytes = new byte[unpackedBytes.length - (unpackedBytes.length / 8)];

            int shiftOffset = (hasUserDataHeader==1)?6:0;
            int shiftIndex = 0;

            // Shift the unpacked bytes to the right according to the offset (position of the byte)
            for (int i=0; i<unpackedBytes.length; i++)
            {
                byte tmpByte = unpackedBytes[i];

                // Handle invalid characters (bytes out of range)
                if (tmpByte > 127)
                {
                    if (!replaceInvalidChars)
                    {
                        // throw exception and exit the method
                        throw new Exception("Invalid character detected: " + tmpByte);
                    }
                    else
                    {
                        tmpByte = defaultByte;
                    }
                }

                // Perform the byte shifting
                if (shiftOffset == 7)
                {
                    shiftOffset = 0;
                }
                else
                {
                    shiftedBytes[shiftIndex] = (byte)(tmpByte >> shiftOffset);
                    shiftOffset++;
                    shiftIndex++;
                }
            }

            int moveOffset = (hasUserDataHeader==1)?7:1;
            int moveIndex = 1;
            int packIndex = 0;
            byte[] packedBytes = new byte[shiftedBytes.length];

            // Move the bits to the appropriate byte (pack the bits)
            for (int i=0; i<unpackedBytes.length; i++)
            {
                if (moveOffset == 8)
                {
                    moveOffset = 1;
                }
                else
                {
                    if (moveIndex != unpackedBytes.length)
                    {
                        // Extract the bits to be moved
                        int extractedBitsByte = (unpackedBytes[moveIndex] & _encodeMask[moveOffset - 1]);
                        // Shift the extracted bits to the proper offset
                        extractedBitsByte = (extractedBitsByte << (8 - moveOffset));
                        // Move the bits to the appropriate byte (pack the bits)
                        int movedBitsByte = (extractedBitsByte | shiftedBytes[packIndex]);

                        packedBytes[packIndex] = (byte)movedBitsByte;

                        moveOffset++;
                        packIndex++;
                    }
                    else
                    {
                        packedBytes[packIndex] = shiftedBytes[packIndex];
                    }
                }

                moveIndex++;
            }

            return packedBytes;
        }


        /// <summary>
        ///  Unpacks a packed 8 bit array to a 7 bit unpacked array according to the GSM
        ///  Protocol.
        /// </summary>
        /// <param name="packedBytes">The byte array that should be unpacked.</param>
        /// <returns>The unpacked bytes array.</returns>
        public static byte[] UnpackBytes(byte[] packedBytes)
        {
            byte[] shiftedBytes = new byte[(packedBytes.length * 8) / 7];

            int shiftOffset = 0;
            int shiftIndex = 0;

            // Shift the packed bytes to the left according to the offset (position of the byte)
            for (int i=0; i<packedBytes.length; i++)
            {
                if (shiftOffset == 7)
                {
                    shiftedBytes[shiftIndex] = 0;
                    shiftOffset = 0;
                    shiftIndex++;
                }

                shiftedBytes[shiftIndex] = (byte)((packedBytes[i] << shiftOffset) & 127);

                shiftOffset++;
                shiftIndex++;
            }

            int moveOffset = 0;
            int moveIndex = 0;
            int unpackIndex = 1;
            byte[] unpackedBytes = new byte[shiftedBytes.length];

            // 
            if (shiftedBytes.length > 0)
            {
                unpackedBytes[unpackIndex - 1] = shiftedBytes[unpackIndex - 1];
            }

            // Move the bits to the appropriate byte (unpack the bits)
            for (int i=0; i<packedBytes.length; i++)
            {
                if (unpackIndex != shiftedBytes.length)
                {
                    if (moveOffset == 7)
                    {
                        moveOffset = 0;
                        unpackIndex++;
                        unpackedBytes[unpackIndex - 1] = shiftedBytes[unpackIndex - 1];
                    }

                    if (unpackIndex != shiftedBytes.length)
                    {
                        // Extract the bits to be moved
                        int extractedBitsByte = (packedBytes[moveIndex] & _decodeMask[moveOffset]);
                        // Shift the extracted bits to the proper offset
                        extractedBitsByte = (extractedBitsByte >> (7 - moveOffset));
                        // Move the bits to the appropriate byte (unpack the bits)
                        int movedBitsByte = (extractedBitsByte | shiftedBytes[unpackIndex]);

                        unpackedBytes[unpackIndex] = (byte)movedBitsByte;

                        moveOffset++;
                        unpackIndex++;
                        moveIndex++;
                    }
                }
            }

            // Remove the padding if exists
            if (unpackedBytes[unpackedBytes.length - 1] == 0)
            {
                byte[] finalResultBytes = new byte[unpackedBytes.length - 1];
                System.arraycopy(unpackedBytes, 0, finalResultBytes, 0, finalResultBytes.length);

                return finalResultBytes;
            }

            return unpackedBytes;
        }


//        /// <summary>
//        /// Converts hex string into the equivalent byte array.
//        /// </summary>
//        /// <param name="hexString">The hex string to be converted.</param>
//        /// <returns>The equivalent byte array.</returns>
//        public static byte[] ConvertHexToBytes(String hexString)
//        {
//            if (hexString.length() % 2 != 0)
//                return null;
//
//            int len = hexString.length() / 2;
//            byte[] array = new byte[len];
//
//            for (int i = 0; i < array.length; i++)
//            {
//                String tmp = hexString.Substring(i * 2, 2);
//                array[i] = byte.Parse(tmp, System.Globalization.NumberStyles.HexNumber);
//            }
//
//            return array;
//        }

//        /// <summary>
//        /// Converts a byte array into the equivalent hex string.
//        /// </summary>
//        /// <param name="byteArray">The byte array to be converted.</param>
//        /// <returns>The equivalent hex string.</returns>
//        public static string ConvertBytesToHex(byte[] byteArray)
//        {
//            if (byteArray == null)
//                return "";
//
//            StringBuilder sb = new StringBuilder();
//            foreach (byte b in byteArray)
//            {
//                sb.Append(b.ToString("X2"));
//            }
//
//            return sb.ToString();
//        }
}

