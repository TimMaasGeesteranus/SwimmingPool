package nl.ru.spp.group5;

import java.util.List;

public class InitTerminal extends Terminal{
    
    private InitTerminal(){

    }


    public static void main(String[] args){
        System.out.println("This is the initial terminal");

        InitTerminal initTerminal = new InitTerminal();
        initTerminal.waitForCard();  
    }
}
