package nl.ru.spp.group5;

import java.util.List;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import nl.ru.spp.group5.Helpers.Utils;

public class InitTerminal extends Terminal{
    
    private InitTerminal(){
        System.out.println("This is the initial terminal");
    }

    public static void main(String[] args){
        InitTerminal initTerminal = new InitTerminal();
        initTerminal.waitForCard();  
    }

    @Override
    public void handleCard(CardChannel channel) throws CardException{
        // exit if card was already issued
        if(isIssued()){
            System.out.println("This card was already issued");
            return;
        }

        int cardID = 12345; // TODO set new cardID everytime


        byte[] pubKeyCard = generateKeysOnCard(channel, cardID);
    }

    private byte[] generateKeysOnCard(CardChannel channel, int cardID) throws CardException{
        System.out.println("generateKeysOnCard");

        // Sending CardID to card
        byte[] data = Utils.intToBytes(cardID);
        System.out.println(data.length);
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x0F, 0x00, 0x00, data);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        // Returning public key from card
        System.out.println(response.getData());
        return response.getData();
    }

    private boolean isIssued(){
        // TODO check if card was already isIssued
        return false;
    }
}