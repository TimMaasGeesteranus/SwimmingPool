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

import static nl.ru.spp.group5.Helpers.Utils.*;

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
        System.out.println("Issueing card. This might take a while...");

        // Generate cardID and cardExpirationDate
        byte[] cardID = generateCardID();

        byte[] cardExpirationDate = getExpirationDate(10);
        byte[] pubKeyCard = generateKeysOnCard(channel, cardID, cardExpirationDate);
        System.out.println("done!");
    }

    private byte[] generateKeysOnCard(CardChannel channel, byte[] cardID, byte[] cardExpirationDate) throws CardException{
        // Making data object from cardID and cardExpirationDate
        byte[] data = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH];
        System.arraycopy(cardID, 0, data, 0, CARD_ID_LENGTH);
        System.arraycopy(cardExpirationDate, 0, data, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);
        
        // Sending data to card
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x0F, 0x00, 0x00, data);

        // Verifying response
         ResponseAPDU response = channel.transmit(apdu);
         if (response.getSW() == 27014){
            System.out.println("Card is already issued and cannot be issued again.");
            System.exit(1);
         }
         else if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
         }

        // Returning public key from card
        System.out.println(response.getData());
        return response.getData();
    }
}