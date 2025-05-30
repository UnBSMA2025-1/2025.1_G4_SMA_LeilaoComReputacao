package Teste;

import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um leilão em andamento para uma obra de arte específica.
 * Gerencia os lances, o maior lance atual e os participantes.
 */
public class Auction {

    /**
     * Estados possíveis de um leilão.
     */
    public enum AuctionState {
        CREATED, // Leilão apenas criado, aguardando início ou lances
        RUNNING, // Leilão em andamento, aceitando lances
        CLOSED   // Leilão encerrado, com um vencedor ou sem venda
    }

    private final String artwork; // A obra de arte sendo leiloada
    private final AID seller;     // O agente vendedor
    private AID highestBidder;    // O agente com o maior lance atual
    private int currentBid;       // O valor do maior lance atual
    private int minNextBid;       // O valor mínimo para o próximo lance válido
    private final List<AID> bidders = new ArrayList<>(); // Lista de agentes que participaram
    private AuctionState state;   // O estado atual do leilão

    /**
     * Construtor para criar um novo leilão.
     *
     * @param artwork O nome da obra de arte.
     * @param seller O AID do agente vendedor.
     * @param startingBid O lance inicial para o leilão.
     */
    public Auction(String artwork, AID seller, int startingBid) {
        this.artwork = artwork;
        this.seller = seller;
        this.currentBid = startingBid; // Define o lance inicial
        this.minNextBid = calculateMinNextBid(startingBid); // Calcula o próximo lance mínimo inicial
        this.state = AuctionState.CREATED; // Inicia no estado CREATED
        this.highestBidder = null; // Nenhum lance ainda
        System.out.println("Leilão criado para '" + artwork + "' por " + seller.getLocalName() + " com lance inicial " + startingBid);
    }

    /**
     * Adiciona um agente à lista de participantes do leilão.
     *
     * @param bidder O AID do agente comprador a ser adicionado.
     */
    public void addBidder(AID bidder) {
        if (!bidders.contains(bidder)) {
            bidders.add(bidder);
            System.out.println(bidder.getLocalName() + " entrou no leilão para '" + artwork + "'.");
        }
    }

    /**
     * Tenta atualizar o leilão com um novo lance.
     * O lance só é aceito se o leilão estiver em andamento e o valor for maior que o lance mínimo exigido.
     *
     * @param bidder O AID do agente que fez o lance.
     * @param bid O valor do lance.
     * @return true se o lance foi aceito, false caso contrário.
     */
    public synchronized boolean placeBid(AID bidder, int bid) {
        if (state != AuctionState.RUNNING) {
            System.out.println("Leilão para '" + artwork + "' não está ativo. Lance de " + bidder.getLocalName() + " rejeitado.");
            return false; // Leilão não está ativo
        }
        if (bid >= minNextBid) {
            System.out.println("Novo lance recebido para '" + artwork + "' de " + bidder.getLocalName() + ": " + bid);
            highestBidder = bidder;
            currentBid = bid;
            minNextBid = calculateMinNextBid(currentBid); // Recalcula o próximo lance mínimo
            addBidder(bidder); // Garante que o licitante está na lista
            System.out.println("Maior lance atualizado para " + currentBid + ". Próximo lance mínimo: " + minNextBid);
            return true;
        } else {
            System.out.println("Lance de " + bid + " por " + bidder.getLocalName() + " para '" + artwork + "' é muito baixo (mínimo: " + minNextBid + "). Rejeitado.");
            return false; // Lance abaixo do mínimo
        }
    }

    /**
     * Calcula o próximo lance mínimo com base no lance atual.
     * Pode ser uma lógica simples (ex: +1) ou mais complexa (ex: +5%, +10).
     *
     * @param currentBidValue O valor do lance atual.
     * @return O valor mínimo para o próximo lance.
     */
    private int calculateMinNextBid(int currentBidValue) {
        // Lógica simples: incrementa em 5% ou 1, o que for maior
        int increment = Math.max(1, (int) Math.round(currentBidValue * 0.05));
        return currentBidValue + increment;
    }

    /**
     * Inicia o leilão, mudando seu estado para RUNNING.
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
     * Fecha o leilão, mudando seu estado para CLOSED.
     * Determina o vencedor (se houver).
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

    // --- Getters ---

    public String getArtwork() {
        return artwork;
    }

    public AID getSeller() {
        return seller;
    }

    public AID getHighestBidder() {
        return highestBidder;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public int getMinNextBid() {
        return minNextBid;
    }

    public List<AID> getBidders() {
        return new ArrayList<>(bidders); // Retorna cópia para evitar modificação externa
    }

    public AuctionState getState() {
        return state;
    }
}
