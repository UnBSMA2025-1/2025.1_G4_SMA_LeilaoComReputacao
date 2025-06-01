package Teste;

import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    public enum AuctionState {
        CREATED, RUNNING, CLOSED
    }

    private final String artwork;
    private final AID seller;
    private AID highestBidder;
    private int currentBid;
    private int minNextBid;
    private final List<AID> bidders = new ArrayList<>();
    private AuctionState state;

    public Auction(String artwork, AID seller, int startingBid) {
        this.artwork = artwork;
        this.seller = seller;
        this.currentBid = startingBid;
        this.minNextBid = calculateMinNextBid(startingBid);
        this.state = AuctionState.CREATED;
        this.highestBidder = null;
        System.out.println("Leilão criado para '" + artwork + "' por " + seller.getLocalName() + " com lance inicial " + startingBid);
    }

    public void addBidder(AID bidder) {
        if (!bidders.contains(bidder)) {
            bidders.add(bidder);
            System.out.println(bidder.getLocalName() + " entrou no leilão para '" + artwork + "'.");
        }
    }

    public synchronized boolean placeBid(AID bidder, int bid) {
        if (state != AuctionState.RUNNING) {
            System.out.println("Leilão para '" + artwork + "' não está ativo. Lance de " + bidder.getLocalName() + " rejeitado.");
            return false;
        }
        if (bid >= minNextBid) {
            System.out.println("Novo lance recebido para '" + artwork + "' de " + bidder.getLocalName() + ": " + bid);
            highestBidder = bidder;
            currentBid = bid;
            minNextBid = calculateMinNextBid(currentBid);
            addBidder(bidder);
            System.out.println("Maior lance atualizado para " + currentBid + ". Próximo lance mínimo: " + minNextBid);
            return true;
        } else {
            System.out.println("Lance de " + bid + " por " + bidder.getLocalName() + " para '" + artwork + "' é muito baixo (mínimo: " + minNextBid + "). Rejeitado.");
            return false;
        }
    }

    private int calculateMinNextBid(int currentBidValue) {
        int increment = Math.max(1, (int) Math.round(currentBidValue * 0.05));
        return currentBidValue + increment;
    }

    public void startAuction() {
        if (state == AuctionState.CREATED) {
            this.state = AuctionState.RUNNING;
            System.out.println("Leilão para '" + artwork + "' iniciado.");
        } else {
            System.out.println("Leilão para '" + artwork + "' não pode ser iniciado (estado atual: " + state + ").");
        }
    }

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

    // Getters...
    public String getArtwork() { return artwork; }
    public AID getSeller() { return seller; }
    public AID getHighestBidder() { return highestBidder; }
    public int getCurrentBid() { return currentBid; }
    public int getMinNextBid() { return minNextBid; }
    public List<AID> getBidders() { return new ArrayList<>(bidders); }
    public AuctionState getState() { return state; }
}