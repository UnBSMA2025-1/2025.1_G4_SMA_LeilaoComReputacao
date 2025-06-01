package Teste;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.Random;

public class Consultant extends Agent {
    private Random random = new Random();
    private int earnings = 0;

    @Override
    protected void setup() {
        System.out.println("[CONSULTANT] " + getAID().getLocalName() + " iniciando...");
        registerService();
        addBehaviour(new ConsultBehaviour());
    }

    private void registerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[CONSULTANT] Serviço registrado com sucesso | Taxa de consulta: $" + ArtConstants.CONSULTATION_FEE);
        } catch (FIPAException fe) {
            System.err.println("[CONSULTANT] Falha no registro: " + fe.getMessage());
            doDelete();
        }
    }

    private class ConsultBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                earnings += ArtConstants.CONSULTATION_FEE;
                System.out.println("[CONSULTANT] Consulta recebida de " + msg.getSender().getLocalName() + 
                                 " | Taxa cobrada: $" + ArtConstants.CONSULTATION_FEE + 
                                 " | Total ganho: $" + earnings);
                
                ACLMessage reply = msg.createReply();
                
                try {
                    String[] parts = msg.getContent().split(";");
                    String artwork = parts[0];
                    int currentPrice = Integer.parseInt(parts[1]);
                    int budget = Integer.parseInt(parts[2]);
                    
                    Thread.sleep(1000 + random.nextInt(2000));
                    
                    double variation = 0.80 + random.nextDouble() * 0.2;
                    int suggestedPrice = (int)(currentPrice * variation);
                    suggestedPrice = Math.min(suggestedPrice, budget);
                    
                    String response = suggestedPrice + ";" + variation;
                    System.out.println("[CONSULTANT] Sugestão enviada: " + response);
                    
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(response);
                } catch (Exception e) {
                    System.err.println("[CONSULTANT] Erro ao processar consulta: " + e.getMessage());
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