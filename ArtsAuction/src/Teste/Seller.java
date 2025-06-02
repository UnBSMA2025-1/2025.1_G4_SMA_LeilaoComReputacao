package Teste;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;

public class Seller extends Agent {
    // Mapa thread-safe para armazenar o catálogo de obras de arte (nome -> preço)
    private ConcurrentHashMap<String, Integer> catalogue = new ConcurrentHashMap<>();
    
    // Conjunto para armazenar os compradores ativos no leilão
    private Set<AID> activeBidders = new HashSet<>();
    
    // Armazena o último comprador restante (quando só resta um)
    private AID lastRemainingBidder = null;
    
    // Armazena a obra de arte atual sendo leiloada
    private String currentArtwork;
    
    // Armazena o lance mais alto atual
    private int currentHighestBid = 0;

    @Override
    protected void setup() {
        System.out.println("[SELLER] " + getAID().getLocalName() + " iniciando...");
        
        // Carrega o catálogo de obras a partir dos argumentos
        loadCatalogue();
        
        // Registra o serviço do vendedor no DF (Directory Facilitator)
        registerService();
        
        // Adiciona um comportamento periódico (executado a cada 1/2 segundos)
        addBehaviour(new TickerBehaviour(this, 500) {
            protected void onTick() {
                // Verifica se há obras no catálogo
                if (!catalogue.isEmpty()) {
                    // Verifica se só resta um comprador ativo
                    if (activeBidders.size() == 1 && lastRemainingBidder == null) {
                        lastRemainingBidder = activeBidders.iterator().next();
                        System.out.println("[SELLER] Único comprador restante: " + 
                                          lastRemainingBidder.getLocalName());
                        // Fecha o leilão com o último comprador
                        closeAuctionWithLastBidder();
                    } else {
                        // Envia CFP (Call For Proposal) para os compradores
                        sendCFP();
                    }
                }
            }
        });
        
        // Adiciona comportamento para lidar com as propostas recebidas
        addBehaviour(new HandleProposalsBehaviour());
    }

    // Método para encerrar o leilão quando só resta um comprador
    private void closeAuctionWithLastBidder() {
        if (lastRemainingBidder != null && !catalogue.isEmpty()) {
            System.out.println("[SELLER] Vendendo para último comprador restante: " + 
                             lastRemainingBidder.getLocalName() + " por $" + currentHighestBid);
            
            // Cria mensagem de aceitação para o último comprador
            ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            acceptMsg.addReceiver(lastRemainingBidder);
            acceptMsg.setContent(currentArtwork + ";" + currentHighestBid);
            acceptMsg.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
            send(acceptMsg);
            
            // Limpa o catálogo e encerra o agente
            catalogue.clear();
            doDelete();
        }
    }

    // Carrega o catálogo a partir dos argumentos passados ao agente
    private void loadCatalogue() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            currentArtwork = (String) args[0];
            int price = Integer.parseInt((String) args[1]);
            catalogue.put(currentArtwork, price);
            currentHighestBid = price; // Define o preço mínimo inicial
            System.out.println("[SELLER] Obra cadastrada: " + currentArtwork + 
                             " - Preço mínimo: $" + price);
        }
    }

    // Registra o serviço do vendedor no DF (Yellow Pages)
    private void registerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_SELLING);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[SELLER] Registrado no DF como vendedor");
        } catch (FIPAException fe) {
            System.err.println("[SELLER] Falha no registro: " + fe.getMessage());
            doDelete();
        }
    }

    // Envia CFP (Call For Proposal) para todos os compradores registrados
    private void sendCFP() {
        try {
            // Template para buscar compradores no DF
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_BUYER);
            template.addServices(sd);
            
            // Busca por compradores registrados
            DFAgentDescription[] results = DFService.search(this, template);
            activeBidders.clear();
            
            if (results.length > 0) {
                System.out.println("[SELLER] Enviando CFP para " + results.length + " comprador(es)");
                
                // Cria mensagem CFP
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                cfp.setContent(currentArtwork + ";" + currentHighestBid);
                cfp.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                
                // Adiciona todos os compradores como destinatários
                for (DFAgentDescription result : results) {
                    cfp.addReceiver(result.getName());
                    activeBidders.add(result.getName());
                }
                
                send(cfp);
            } else {
                System.out.println("[SELLER] Nenhum comprador encontrado");
            }
        } catch (FIPAException fe) {
            System.err.println("[SELLER] Erro ao buscar compradores: " + fe.getMessage());
        }
    }

    // Comportamento para lidar com as propostas recebidas dos compradores
    private class HandleProposalsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Template para receber apenas mensagens PROPOSE com a ontologia correta
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            // Recebe a mensagem (se houver)
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                // Extrai conteúdo da mensagem
                String[] parts = msg.getContent().split(";");
                String artwork = parts[0];
                int offer = Integer.parseInt(parts[1]);
                
                // Verifica se a obra é a que está sendo leiloada
                if (artwork.equals(currentArtwork)) {
                    // Atualiza o lance mais alto
                    currentHighestBid = offer;
                    System.out.println("[SELLER] Novo lance recebido de " + 
                                     msg.getSender().getLocalName() + ": $" + offer);
                    
                    // Adiciona o comprador à lista de ativos (se ainda não estiver)
                    if (!activeBidders.contains(msg.getSender())) {
                        activeBidders.add(msg.getSender());
                    }
                }
            } else {
                // Bloqueia o comportamento até receber nova mensagem
                block();
            }
        }
    }
}