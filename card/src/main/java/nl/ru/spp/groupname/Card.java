package nl.ru.spp.groupname;

import javacard.framework.*;


public class Card extends Applet implements ISO7816 {


    Card() {
    
    	register();

    }
    
    public static void install(byte[] buffer, short offset, byte length) throws SystemException {
        new Card();
    }
    
    public void process(APDU apdu) throws ISOException {
    }
        
}
