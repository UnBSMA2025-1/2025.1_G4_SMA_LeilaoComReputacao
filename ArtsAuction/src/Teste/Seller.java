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
import java.util.Map;

public class Seller extends Agent {
    private Map<String, Integer> catalogue = new ConcurrentHashMap<>();
    private boolean itemSold = false;

    @Override
    protected void setup() {
        System.out.println("[SELLER] " + getAID().getLocalName() + " iniciando...");
        loadCatalogueFromArgs(getArguments());
        registerService();
        
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                if (!itemSold && !catalogue.isEmpty()) {
                    sendCFP();
                } else if (itemSold) {
                    System.out.println("[SELLER] Item vendido - encerrando leilões");
                    doDelete();
                } else {
                    System.out.println("[SELLER] Catálogo vazio - nenhuma obra disponível");
                }
            }
        });
        
        addBehaviour(new HandleProposalsBehaviour());
    }

    private void loadCatalogueFromArgs(Object[] args) {
        System.out.println("[SELLER] Carregando catálogo...");
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; i += 2) {
                try {
                    String artwork = (String) args[i];
                    int price = Integer.parseInt((String) args[i+1]);
                    catalogue.put(artwork, price);
                    System.out.println("[SELLER] Adicionada: " + artwork + " | Preço mínimo: $" + price);
                } catch (Exception e) {
                    System.err.println("[SELLER] Erro ao carregar item: " + e.getMessage());
                }
            }
        }
        System.out.println("[SELLER] Catálogo carregado. Total de obras: " + catalogue.size());
    }

    private void registerService() {
        System.out.println("[SELLER] Registrando serviço no DF...");
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_SELLING);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[SELLER] Serviço registrado com sucesso");
        } catch (FIPAException fe) {
            System.err.println("[SELLER] Falha no registro: " + fe.getMessage());
            doDelete();
        }
    }

    private void sendCFP() {
        String artwork = catalogue.keySet().iterator().next();
        int price = catalogue.get(artwork);
        
        System.out.println("[SELLER] Enviando CFP para: " + artwork + " | Preço mínimo: $" + price);
        
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.addReceiver(new AID("Buyer1", AID.ISLOCALNAME));
        cfp.setContent(artwork + ";" + price);
        cfp.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
        send(cfp);
    }

    private class HandleProposalsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                System.out.println("[SELLER] Proposta recebida de " + msg.getSender().getLocalName());
                
                ACLMessage reply = msg.createReply();
                reply.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                
                try {
                    String[] parts = msg.getContent().split(";");
                    String artwork = parts[0];
                    int offer = Integer.parseInt(parts[1]);
                    
                    System.out.println("[SELLER] Oferta para " + artwork + ": $" + offer);
                    
                    if (!catalogue.containsKey(artwork)) {
                        System.out.println("[SELLER] Obra não disponível");
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("Obra não disponível");
                    } else if (offer >= catalogue.get(artwork)) {
                        catalogue.remove(artwork);
                        itemSold = true;
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent("Venda aprovada");
                        System.out.println("[SELLER] Venda concluída: " + artwork + " por $" + offer);
                    } else {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Oferta abaixo do mínimo");
                        System.out.println("[SELLER] Oferta recusada ($" + offer + " < $" + catalogue.get(artwork) + ")");
                    }
                } catch (Exception e) {
                    System.err.println("[SELLER] Erro ao processar proposta: " + e.getMessage());
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Erro: " + e.getMessage());
                }
                
                send(reply);
            } else {
                block();
            }
        }
    }
}