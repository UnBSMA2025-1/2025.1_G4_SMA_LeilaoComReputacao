package Teste;

import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa um leilão de uma obra de arte.
 * Gerencia todo o ciclo de vida do leilão, desde sua criação até o fechamento.
 */
public class Auction {
    // Enumeração que define os possíveis estados de um leilão
    public enum AuctionState {
        CREATED,   // Leilão criado mas não iniciado
        RUNNING,   // Leilão em andamento aceitando lances
        CLOSED     // Leilão finalizado
    }

    // Atributos do leilão
    private final String artwork;      // Nome/identificação da obra de arte
    private final AID seller;          // Agente vendedor responsável pelo leilão
    private AID highestBidder;         // Agente com o maior lance atual
    private int currentBid;            // Valor do maior lance atual
    private int minNextBid;            // Valor mínimo para o próximo lance
    private final List<AID> bidders;   // Lista de participantes do leilão
    private AuctionState state;        // Estado atual do leilão

    /**
     * Construtor do leilão.
     * @param artwork Nome/identificação da obra de arte
     * @param seller Agente vendedor responsável
     * @param startingBid Valor inicial do lance
     */
    public Auction(String artwork, AID seller, int startingBid) {
        this.artwork = artwork;
        this.seller = seller;
        this.currentBid = startingBid;
        this.minNextBid = calculateMinNextBid(startingBid); // Calcula o próximo lance mínimo
        this.state = AuctionState.CREATED; // Estado inicial
        this.highestBidder = null; // Sem vencedor inicial
        this.bidders = new ArrayList<>(); // Lista vazia de participantes
        System.out.println("Leilão criado para '" + artwork + "' por " + seller.getLocalName() + " com lance inicial " + startingBid);
    }

    /**
     * Adiciona um participante ao leilão.
     * @param bidder Agente participante a ser adicionado
     */
    public void addBidder(AID bidder) {
        if (!bidders.contains(bidder)) {
            bidders.add(bidder);
            System.out.println(bidder.getLocalName() + " entrou no leilão para '" + artwork + "'.");
        }
    }

    /**
     * Registra um novo lance no leilão (método sincronizado para thread safety).
     * @param bidder Agente que está fazendo o lance
     * @param bid Valor do lance
     * @return true se o lance foi aceito, false caso contrário
     */
    public synchronized boolean placeBid(AID bidder, int bid) {
        // Verifica se o leilão está ativo
        if (state != AuctionState.RUNNING) {
            System.out.println("Leilão para '" + artwork + "' não está ativo. Lance de " + bidder.getLocalName() + " rejeitado.");
            return false;
        }
        
        // Verifica se o lance atende ao valor mínimo
        if (bid >= minNextBid) {
            System.out.println("Novo lance recebido para '" + artwork + "' de " + bidder.getLocalName() + ": " + bid);
            // Atualiza informações do leilão
            highestBidder = bidder;
            currentBid = bid;
            minNextBid = calculateMinNextBid(currentBid);
            addBidder(bidder); // Garante que o participante está na lista
            
            System.out.println("Maior lance atualizado para " + currentBid + ". Próximo lance mínimo: " + minNextBid);
            return true;
        } else {
            System.out.println("Lance de " + bid + " por " + bidder.getLocalName() + " para '" + artwork + "' é muito baixo (mínimo: " + minNextBid + "). Rejeitado.");
            return false;
        }
    }

    /**
     * Calcula o valor mínimo para o próximo lance com base no lance atual.
     * @param currentBidValue Valor do lance atual
     * @return Valor mínimo para o próximo lance
     */
    private int calculateMinNextBid(int currentBidValue) {
        // Incremento de 5% do valor atual (mínimo de 1 unidade)
        int increment = Math.max(1, (int) Math.round(currentBidValue * 0.05));
        return currentBidValue + increment;
    }

    /**
     * Inicia o leilão, permitindo a aceitação de lances.
     */
    public void startAuction() {
        if (state == AuctionState.CREATED) {
            this.state = AuctionState.RUNNING;
            System.out.println("Leilão para '" + artwork + "' iniciado.");
        } else {
            System.out.println("Leilão para '" + artwork + "' não pode ser iniciado (estado atual: " + state + ").");
        }
    }

    /**
     * Finaliza o leilão, anunciando o vencedor (se houver).
     */
    public void closeAuction() {
        if (state == AuctionState.RUNNING) {
            this.state = AuctionState.CLOSED;
            if (highestBidder != null) {
                System.out.println("Leilão para '" + artwork + "' encerrado. Vencedor: " + highestBidder.getLocalName() + " com lance de " + currentBid);
            } else {
                System.out.println("Leilão para '" + artwork + "' encerrado sem lances ou vencedor.");
            }
        } else {
             System.out.println("Leilão para '" + artwork + "' não pode ser fechado (estado atual: " + state + ").");
        }
    }

    // Métodos getters para acesso às informações do leilão
    
    /**
     * @return Nome/identificação da obra de arte
     */
    public String getArtwork() { return artwork; }
    
    /**
     * @return Agente vendedor responsável
     */
    public AID getSeller() { return seller; }
    
    /**
     * @return Agente com o maior lance atual
     */
    public AID getHighestBidder() { return highestBidder; }
    
    /**
     * @return Valor do maior lance atual
     */
    public int getCurrentBid() { return currentBid; }
    
    /**
     * @return Valor mínimo para o próximo lance
     */
    public int getMinNextBid() { return minNextBid; }
    
    /**
     * @return Lista de participantes do leilão (cópia para evitar modificações externas)
     */
    public List<AID> getBidders() { return new ArrayList<>(bidders); }
    
    /**
     * @return Estado atual do leilão
     */
    public AuctionState getState() { return state; }
}