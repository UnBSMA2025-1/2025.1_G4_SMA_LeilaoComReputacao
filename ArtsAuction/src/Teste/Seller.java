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

public class Seller extends Agent {
    private ConcurrentHashMap<String, Integer> catalogue = new ConcurrentHashMap<>();
    private boolean itemSold = false;

    @Override
    protected void setup() {
        System.out.println("[SELLER] " + getAID().getLocalName() + " iniciando...");
        loadCatalogue();
        registerService();
        
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                if (!itemSold && !catalogue.isEmpty()) {
                    sendCFP();
                } else if (itemSold) {
                    System.out.println("[SELLER] Item vendido - encerrando");
                    doDelete();
                }
            }
        });
        
        addBehaviour(new HandleProposalsBehaviour());
    }

    private void loadCatalogue() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            String artwork = (String) args[0];
            int price = Integer.parseInt((String) args[1]);
            catalogue.put(artwork, price);
            System.out.println("[SELLER] Obra cadastrada: " + artwork + " - Preço mínimo: $" + price);
        }
    }

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

    private void sendCFP() {
        String artwork = catalogue.keySet().iterator().next();
        int price = catalogue.get(artwork);
        
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_BUYER);
            template.addServices(sd);
            
            DFAgentDescription[] results = DFService.search(this, template);
            
            if (results.length > 0) {
                System.out.println("[SELLER] Enviando CFP para " + results.length + " comprador(es)");
                
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                cfp.setContent(artwork + ";" + price);
                cfp.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                
                for (DFAgentDescription result : results) {
                    cfp.addReceiver(result.getName());
                }
                
                send(cfp);
            } else {
                System.out.println("[SELLER] Nenhum comprador encontrado no momento");
            }
        } catch (FIPAException fe) {
            System.err.println("[SELLER] Erro ao buscar compradores: " + fe.getMessage());
        }
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
                String[] parts = msg.getContent().split(";");
                String artwork = parts[0];
                int offer = Integer.parseInt(parts[1]);
                
                ACLMessage reply = msg.createReply();
                reply.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                
                if (catalogue.containsKey(artwork)) {
                    if (offer >= catalogue.get(artwork)) {
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
                } else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Obra não disponível");
                    System.out.println("[SELLER] Obra não disponível");
                }
                
                send(reply);
            } else {
                block();
            }
        }
    }
}