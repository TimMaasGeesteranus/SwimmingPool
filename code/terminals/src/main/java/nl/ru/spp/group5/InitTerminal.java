package nl.ru.spp.group5;

import java.util.List;

import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

public class InitTerminal extends Terminal{
    
    private InitTerminal(){

    }


    public static void main(String[] args){
        System.out.println("This is the initial terminal");

        InitTerminal initTerminal = new InitTerminal();
        initTerminal.waitForCard();  
    }

    @Override
    public void handleCard(CardChannel channel) throws CardException{

    }
}
