package nl.ru.swimmingpool.card;

import javacard.framework.*;

public class Card extends Applet implements ISO7816 {

    // Instruction codes
    private static final byte INS_GET_DATA = (byte) 0x00;
    private static final byte INS_SET_DATA = (byte) 0x01;

    // Data buffer
    private byte[] data;
    private byte entryCounter;

    // Constructor
    Card() {
        // Initialize buffer and entry counter
        data = new byte[256];
        entryCounter = 10; 
        register();
    }

    // Install method
    public static void install(byte[] buffer, short offset, byte length) throws SystemException {
        new Card();
    }

    // Process method
    public void process(APDU apdu) throws ISOException {
        byte[] buffer = apdu.getBuffer();
        if (selectingApplet()) {
            return;
        }

        byte ins = buffer[ISO7816.OFFSET_INS];
        switch (ins) {
            case INS_GET_DATA:
                getData(apdu);
                break;
            case INS_SET_DATA:
                setData(apdu);
                break;
            default:
                // Respond with an error for unsupported instructions
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // Get data method
    private void getData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = (short) data.length;
        Util.arrayCopy(data, (short) 0, buffer, ISO7816.OFFSET_CDATA, length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

    // Set data method
    private void setData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, data, (short) 0, length);
    }

    // Issue season ticket method
    public void issueSeasonTicket(int duration) {
        // Set the expiration date of the season ticket
        JCSystem.getCurrentDate(this.expirationYear, this.expirationMonth, this.expirationDay);
        Util.addDay(this.expirationYear, this.expirationMonth, this.expirationDay, duration);
        System.out.println("Issuing a season ticket valid until " + expirationYear + "-" + expirationMonth + "-" + expirationDay + ".");
    }

    // Check if season ticket is valid
    public boolean isSeasonTicketValid() {
        short currentYear;
        byte currentMonth;
        byte currentDay;
        JCSystem.getCurrentDate(currentYear, currentMonth, currentDay);
        return Util.compareDate(currentYear, currentMonth, currentDay, expirationYear, expirationMonth, expirationDay) <= 0;
    }

    // Issue 10-entry ticket method
    public void issueEntryTicket() {
        entryCounter = 10;
        System.out.println("Issuing a 10-entry ticket with 10 entries.");
    }

    // Use entry method
    public void useEntry() {
        if (entryCounter > 0) {
            entryCounter--;
            System.out.println("One entry used, " + entryCounter + " entries remaining.");
        } else {
            System.out.println("No entries remaining. Please reload or issue a new ticket.");
        }
    }

    // Check remaining entries
    public byte getRemainingEntries() {
        return entryCounter;
    }
}
