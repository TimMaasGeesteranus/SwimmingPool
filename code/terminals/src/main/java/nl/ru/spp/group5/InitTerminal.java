package nl.ru.spp.group5;

import java.util.List;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import static nl.ru.spp.group5.Helpers.Utils.*;

public class InitTerminal extends Terminal{
    
    private InitTerminal() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        System.out.println("This is the initial terminal");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        InitTerminal initTerminal = new InitTerminal();
        initTerminal.waitForCard();  
    }

    @Override
    public void handleCard(CardChannel channel) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CardException{
        System.out.println("Issueing card. This might take a while...");

        // Generate cardID and cardExpirationDate
        byte[] cardID = generateCardID();
        byte[] cardExpirationDate = getExpirationDate(10);

        // Send ID+Expirationdate, generate keys on card and receive pubKeyCard
        byte[] pubKeyCard = generateKeysOnCard(channel, cardID, cardExpirationDate);

        // Generate certificate and send to card
        byte[] cert = generateCert(cardID, cardExpirationDate, pubKeyCard);
        sendCertToCard(channel, cert);

        // Print receipt
        printReceipt(cardID, cardExpirationDate);    
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

    private byte[] generateCert(byte[] cardID, byte[] cardExpirationDate, byte[] pubKeyCard) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException{
        // Concatenate cardID, cardExpirationdate and pubKeyCard
        byte[] dataToSign = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH + KEY_LENGTH];
        System.arraycopy(cardID, 0, dataToSign, 0, CARD_ID_LENGTH);
        System.arraycopy(cardExpirationDate, 0, dataToSign, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);
        System.arraycopy(pubKeyCard, 0, dataToSign, CARD_ID_LENGTH+CARD_EXP_DATE_LENGTH, KEY_LENGTH);
        
        // Sign and return
        return sign(dataToSign, TERMINAL_PRIV_KEY);
    }

    private void sendCertToCard(CardChannel channel, byte[] cert){

    }

    private void printReceipt(byte[] cardID, byte[] cardExpirationDate){
        System.out.println("RECEIPT");
    }
}