package Teste;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.UUID;

public class Buyer extends Agent {
    private String targetArtwork;
    private int maxBudget;
    private AID consultantAID;
    private AID currentSeller;
    private int currentPrice;
    private int suggestedPrice;
    private String conversationId;

    @Override
    protected void setup() {
        System.out.println("[BUYER] " + getAID().getLocalName() + " iniciando...");
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            targetArtwork = (String) args[0];
            maxBudget = Integer.parseInt((String) args[1]);
            System.out.println("[BUYER] Procurando por: " + targetArtwork + " | Orçamento: $" + maxBudget);
            
            addBehaviour(new FindConsultantBehaviour());
            addBehaviour(new HandleCFPBehaviour());
        } else {
            System.err.println("[BUYER] Argumentos inválidos");
            doDelete();
        }
    }

    private class FindConsultantBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("[BUYER] Procurando por especialista...");
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);
                template.addServices(sd);
                
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    consultantAID = result[0].getName();
                    System.out.println("[BUYER] Especialista encontrado: " + consultantAID.getLocalName());
                } else {
                    System.out.println("[BUYER] Nenhum especialista encontrado");
                }
            } catch (FIPAException fe) {
                System.err.println("[BUYER] Erro na busca: " + fe.getMessage());
            }
        }
    }

    private class HandleCFPBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                System.out.println("[BUYER] CFP recebido de " + msg.getSender().getLocalName());
                
                String[] contentParts = msg.getContent().split(";");
                String artwork = contentParts[0];
                currentPrice = Integer.parseInt(contentParts[1]);
                
                if (artwork.equals(targetArtwork)) {
                    currentSeller = msg.getSender();
                    System.out.println("[BUYER] Obra desejada: " + artwork + " | Preço: $" + currentPrice);
                    
                    conversationId = "consult-" + UUID.randomUUID().toString();
                    addBehaviour(new ConsultExpertBehaviour());
                } else {
                    System.out.println("[BUYER] Ignorando CFP - obra não corresponde");
                }
            } else {
                block();
            }
        }
    }

    private class ConsultExpertBehaviour extends SequentialBehaviour {
        public ConsultExpertBehaviour() {
            super();
            
            // 1. Envio da consulta ao especialista
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    if (consultantAID != null) {
                        System.out.println("[BUYER] Enviando consulta para especialista");
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(consultantAID);
                        request.setContent(targetArtwork + ";" + currentPrice + ";" + maxBudget);
                        request.setConversationId(conversationId);
                        request.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                        send(request);
                    } else {
                        System.out.println("[BUYER] Nenhum especialista disponível");
                        suggestedPrice = currentPrice;
                        addBehaviour(new MakeOfferBehaviour());
                    }
                }
            });
            
            // 2. Espera pela resposta (implementação corrigida)
            addSubBehaviour(new CyclicBehaviour(myAgent) {
                private boolean messageReceived = false;
                private final MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(conversationId),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );

                @Override
                public void action() {
                    if (!messageReceived) {
                        ACLMessage msg = myAgent.receive(mt);
                        if (msg != null) {
                            System.out.println("[BUYER] Resposta do especialista recebida: " + msg.getContent());
                            String[] parts = msg.getContent().split(";");
                            suggestedPrice = Integer.parseInt(parts[1]);
                            messageReceived = true;
                            addBehaviour(new MakeOfferBehaviour());
                        } else {
                            block();
                        }
                    }
                }

                @Override
                public int onEnd() {
                    return 0; // Comportamento concluído
                }
            });
        }
    }

    private class MakeOfferBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            suggestedPrice = Math.min(suggestedPrice, maxBudget);
            System.out.println("[BUYER] Fazendo oferta de $" + suggestedPrice + " para " + currentSeller.getLocalName());
            
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(currentSeller);
            propose.setContent(targetArtwork + ";" + suggestedPrice);
            propose.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
            send(propose);
            
            addBehaviour(new WaitForResponseBehaviour());
        }
    }

    private class WaitForResponseBehaviour extends Behaviour {
        private boolean responseReceived = false;
        private final MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchSender(currentSeller),
            MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
        );
        
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                responseReceived = true;
                System.out.println("[BUYER] Resposta do vendedor: " + ACLMessage.getPerformative(msg.getPerformative()));
                
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("[BUYER] Compra realizada: " + targetArtwork + " por $" + suggestedPrice);
                    doDelete();
                } else {
                    System.out.println("[BUYER] Oferta recusada: " + msg.getContent());
                }
            } else {
                block();
            }
        }
        
        @Override
        public boolean done() {
            return responseReceived;
        }
    }
}