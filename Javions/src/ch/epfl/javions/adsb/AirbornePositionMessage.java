package ch.epfl.javions.adsb;

import ch.epfl.javions.Bits;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.Units;
import ch.epfl.javions.aircraft.IcaoAddress;

/**
 * Position message of an aircraft
 *
 * @author Tuan Dang Nguyen (361089)
 * @author Rayane Charif Chefchouni (339839)
 */
public record AirbornePositionMessage(long timeStampNs, IcaoAddress icaoAddress, double altitude, int parity,
                                      double x, double y) implements Message {

    /**
     * Construct an airborne position message
     *
     * @param timeStampNs of the aircraft
     * @param icaoAddress of the aircraft
     * @param altitude    of the aircraft
     * @param parity      of message
     * @param x           : local longitude normalized
     * @param y           : local latitude normalized
     * @throws NullPointerException     if the ICAO address of the aircraft is null
     * @throws IllegalArgumentException if time stamps is negative or the parity isn't 0 nor 1
     * @throws IllegalArgumentException if the value of local longitude and latitude aren't between 0 (included) and 1 (excluded)
     */
    public AirbornePositionMessage {
        if (icaoAddress == null) {
            throw new NullPointerException("IcaoAddress is null");
        }
        Preconditions.checkArgument(timeStampNs >= 0);
        Preconditions.checkArgument(parity == 0 || parity == 1);
        Preconditions.checkArgument(x >= 0 && x < 1);
        Preconditions.checkArgument(y >= 0 && y < 1);
    }

    /**
     * Get a position message of the aircraft
     *
     * @param rawMessage of the aircraft
     * @return position message of the aircraft or null if the message is invalid
     */
    public static AirbornePositionMessage of(RawMessage rawMessage) {
        return Double.isNaN(altitudeCalculator(rawMessage)) ?
                null : new AirbornePositionMessage(rawMessage.timeStampNs(), rawMessage.icaoAddress(),
                altitudeCalculator(rawMessage), determineParity(rawMessage), LAT_CPR(rawMessage), LON_CPR(rawMessage));
    }

    /**
     * Get the time stamp of the current position message
     *
     * @return time stamp of the current position message
     */
    @Override
    public long timeStampNs() {
        return timeStampNs;
    }

    /**
     * Get the ICAO address of the current position message
     *
     * @return ICAO address of the current position message
     */
    @Override
    public IcaoAddress icaoAddress() {
        return icaoAddress;
    }

    /**
     * Calculate and normalize latitude from a raw message of the aircraft
     *
     * @param rawMessage of the aircraft
     * @return latitude normalized extracted from raw message
     */
    private static double LAT_CPR(RawMessage rawMessage) {
        return Math.scalb(Bits.extractUInt(rawMessage.payload(), 0, 17), -17);
    }

    /**
     * Calculate and normalize longitude from raw message of the aircraft
     *
     * @param rawMessage of the aircraft
     * @return longitude normalized extracted
     */
    private static double LON_CPR(RawMessage rawMessage) {
        return Math.scalb(Bits.extractUInt(rawMessage.payload(), 17, 17), -17);
    }

    /**
     * Calculate parity from raw message of the aircraft
     *
     * @param rawMessage of the aircraft
     * @return the parity extracted
     */
    private static int determineParity(RawMessage rawMessage) {
        return Bits.extractUInt(rawMessage.payload(), 34, 1);
    }

    /**
     * Calculate altitude of the aircraft
     *
     * @param rawMessage of the aircraft
     * @return altitude of the aircraft
     */
    private static double altitudeCalculator(RawMessage rawMessage) {
        int alt = Bits.extractUInt(rawMessage.payload(), 36, 12);

        if (determineQ(rawMessage) == 1) {
            return Units.convert((double) -1000 + removeBitForQ1(alt) * 25, Units.Length.FOOT, Units.Length.METER);
        } else {
            //Un-scramble
            alt = unscrambled(alt);

            //Divide in 2 groups
            int group1 = Bits.extractUInt(alt, 0, 3);
            int group2 = Bits.extractUInt(alt, 3, 9);

            //From Gray --> Real Value
            group1 = GrayToValue(group1, 3);
            group2 = GrayToValue(group2, 9);

            if (group1 == 0 || group1 == 5 || group1 == 6) {
                return Double.NaN;
            }
            if (group1 == 7) {
                group1 = 5;
            }
            if (group2 % 2 == 1) {
                group1 = 6 - group1;
            }

            return Units.convert(-1300 + group1 * 100 + group2 * 500, Units.Length.FOOT, Units.Length.METER);
        }
    }

    /**
     * Determine bit Q (in order to calculate the altitude)
     *
     * @param rawMessage of the aircraft
     * @return value of bit Q of the aircraft
     */
    private static int determineQ(RawMessage rawMessage) {
        return Bits.extractUInt(Bits.extractUInt(rawMessage.payload(), 36, 12), 4, 1);
    }

    /**
     * Calculate the value of the 12 bits of attribute ALT when Q = 1
     *
     * @param num : attribute ALT extracted from raw message of the aircraft
     * @return the value of attribute ALT upon removing bit Q
     */
    private static int removeBitForQ1(int num) {
        int mask = (1 << 4) - 1;
        return (char) ((num & ((~mask) << 1)) >>> 1) | (num & mask);
    }

    /**
     * Unscramble the 12 bits of attribute ALT
     * An operation in order to calculate the altitude of the aircraft when Q = 0
     *
     * @param b : attribute ALT extracted from raw message of the aircraft
     * @return the unscrambled
     */
    private static int unscrambled(int b) {
        int swapped = 0;
        int[] bitOrder = {7, 9, 11, 1, 3, 5, 6, 8, 10, 0, 2, 4};
        for (int i = 0; i < bitOrder.length; i++) {
            int bit = (b >> bitOrder[i]) & 1;
            bit <<= i;
            swapped |= bit;
        }
        return swapped;
    }

    /**
     * Decode Gray's code
     *
     * @param gray's  code
     * @param nb_bits : number of bits that will be used to decode gray's code
     * @return the decoded bits from Gray's code
     */
    private static int GrayToValue(int gray, int nb_bits) {
        int a = gray;
        for (int i = 1; i < nb_bits; ++i) {
            a = a ^ (gray >> i);
        }
        return a;
    }

}


