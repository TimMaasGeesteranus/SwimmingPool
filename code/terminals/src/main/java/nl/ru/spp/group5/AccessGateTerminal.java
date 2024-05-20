package nl.ru.spp.group5;

import java.util.Scanner;
import javax.smartcardio.*;

public class AccessGateTerminal extends Terminal{
    public static void main(String[] args){
        System.out.println("This is the access gate terminal");
        AccessGateTerminal accessGateTerminal = new AccessGateTerminal();
        accessGateTerminal.waitForCard();
    }

    public AccessGateTerminal(){

    }

    @Override
    public void handleCard(CardChannel channel) throws CardException{
        // TODO: Mutual authentication

        CommandAPDU apdu = new CommandAPDU((byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00); // TODO: what bytes are sent when?
        ResponseAPDU response = channel.transmit(apdu);

        byte[] responseBytes = response.getData();
        String responseString = new String(responseBytes);
    
        switch (responseString) { // TODO: set meaning for bytes
            case "true":
                openGate();       
                break;
            case "false":
                denyAccess();
                break;
            default:
                denyAccess();
                break;
        }
    }

    public void openGate(){
        System.out.println("Welcome to the swimming pool!");
        System.out.println("...Opening the gate...");

        // TODO: add logging
    }

    public void denyAccess(){
        System.out.println("Access denied...");

        // TODO: add logging
    }
}
