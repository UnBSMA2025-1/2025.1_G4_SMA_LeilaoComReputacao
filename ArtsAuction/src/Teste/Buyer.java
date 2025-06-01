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
    private int availableBudget;
    private AID consultantAID;
    private AID currentSeller;
    private int currentPrice;
    private int suggestedPrice;
    private String conversationId;
    private boolean consulted = false;

    @Override
    protected void setup() {
        System.out.println("[BUYER] " + getAID().getLocalName() + " iniciando...");
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            targetArtwork = (String) args[0];
            maxBudget = Integer.parseInt((String) args[1]);
            availableBudget = maxBudget;
            System.out.println("[BUYER] Procurando por: " + targetArtwork + " | Orçamento total: $" + maxBudget);
            
            registerBuyerService();
            addBehaviour(new FindSellerBehaviour());
        } else {
            System.err.println("[BUYER] Argumentos inválidos");
            doDelete();
        }
    }

    private void registerBuyerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_BUYER);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[BUYER] Registrado no DF como comprador");
        } catch (FIPAException fe) {
            System.err.println("[BUYER] Falha ao registrar: " + fe.getMessage());
        }
    }

    private class FindSellerBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("[BUYER] Procurando por vendedor...");
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(ArtConstants.SERVICE_TYPE_SELLING);
                template.addServices(sd);
                
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    currentSeller = result[0].getName();
                    System.out.println("[BUYER] Vendedor encontrado: " + currentSeller.getLocalName());
                    addBehaviour(new FindConsultantBehaviour());
                } else {
                    System.out.println("[BUYER] Nenhum vendedor encontrado");
                }
            } catch (FIPAException fe) {
                System.err.println("[BUYER] Erro na busca: " + fe.getMessage());
            }
        }
    }

    private class FindConsultantBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("[BUYER] Procurando por consultor...");
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);
                template.addServices(sd);
                
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    consultantAID = result[0].getName();
                    System.out.println("[BUYER] Consultor encontrado: " + consultantAID.getLocalName());
                } else {
                    System.out.println("[BUYER] Nenhum consultor encontrado");
                }
            } catch (FIPAException fe) {
                System.err.println("[BUYER] Erro na busca: " + fe.getMessage());
            }
            addBehaviour(new HandleCFPBehaviour());
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
                    System.out.println("[BUYER] Obra desejada: " + artwork + " | Preço: $" + currentPrice + 
                                     " | Orçamento disponível: $" + availableBudget);
                    
                    if (!consulted) {
                        conversationId = UUID.randomUUID().toString();
                        addBehaviour(new ConsultExpertBehaviour());
                    } else {
                        makeOffer();
                    }
                }
            } else {
                block();
            }
        }
        private void makeOffer() {
            // Garante que a oferta não ultrapasse o orçamento disponível
            suggestedPrice = Math.min(suggestedPrice, availableBudget);
            
            System.out.println("[BUYER] Fazendo oferta de $" + suggestedPrice + 
                             " (Orçamento restante: $" + (availableBudget - suggestedPrice) + ")");
            
            // Prepara a mensagem de proposta
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(currentSeller);
            propose.setContent(targetArtwork + ";" + suggestedPrice);
            propose.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
            
            // Envia a proposta
            send(propose);
            
            // Configura o template para esperar a resposta do Seller
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchSender(currentSeller),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            
            // Aguarda a resposta do Seller com timeout de 5 segundos
            ACLMessage response = blockingReceive(mt, 5000);
            
            if (response != null) {
                // Oferta aceita
                System.out.println("[BUYER] Oferta aceita! Compra realizada por $" + suggestedPrice);
                doDelete(); // Encerra o agente após compra bem-sucedida
            } else {
                // Oferta recusada ou timeout
                System.out.println("[BUYER] Oferta recusada ou timeout");
                
                // Se quiser implementar lógica de aumento progressivo:
                // suggestedPrice = Math.min(suggestedPrice + incremento, availableBudget);
            }
        }
    private class ConsultExpertBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            if (consultantAID != null && availableBudget >= ArtConstants.CONSULTATION_FEE) {
                System.out.println("[BUYER] Pagando taxa de consulta de $" + ArtConstants.CONSULTATION_FEE);
                availableBudget -= ArtConstants.CONSULTATION_FEE;
                System.out.println("[BUYER] Orçamento após taxa: $" + availableBudget);
                
                System.out.println("[BUYER] Consultando especialista...");
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(consultantAID);
                request.setContent(targetArtwork + ";" + currentPrice + ";" + availableBudget);
                request.setConversationId(conversationId);
                request.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                send(request);
                
                ACLMessage reply = blockingReceive(
                    MessageTemplate.and(
                        MessageTemplate.MatchConversationId(conversationId),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                    ), 
                    ArtConstants.CONSULTANT_TIMEOUT
                );
                
                if (reply != null) {
                    String[] parts = reply.getContent().split(";");
                    suggestedPrice = Integer.parseInt(parts[0]);
                    System.out.println("[BUYER] Sugestão recebida: $" + suggestedPrice);
                    consulted = true;
                } else {
                    System.out.println("[BUYER] Timeout na consulta ao especialista");
                    suggestedPrice = currentPrice;
                }
            } else if (consultantAID == null) {
                System.out.println("[BUYER] Nenhum consultor disponível - usando preço atual");
                suggestedPrice = currentPrice;
            } else {
                System.out.println("[BUYER] Orçamento insuficiente para consulta (necessário $" + 
                                 ArtConstants.CONSULTATION_FEE + ")");
                suggestedPrice = currentPrice;
            }
            makeOffer();
        }

        private void makeOffer() {
            suggestedPrice = Math.min(suggestedPrice, availableBudget);
            System.out.println("[BUYER] Fazendo oferta de $" + suggestedPrice + 
                             " (Orçamento restante: $" + (availableBudget - suggestedPrice) + ")");
            
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(currentSeller);
            propose.setContent(targetArtwork + ";" + suggestedPrice);
            propose.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
            send(propose);
            
            ACLMessage response = blockingReceive(
                MessageTemplate.and(
                    MessageTemplate.MatchSender(currentSeller),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
                ),
                5000
            );
            
            if (response != null) {
                System.out.println("[BUYER] Oferta aceita! Compra realizada por $" + suggestedPrice);
                doDelete();
            } else {
                System.out.println("[BUYER] Oferta recusada ou timeout");
            }
        }
    }
    }}