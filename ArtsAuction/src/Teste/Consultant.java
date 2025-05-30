package Teste;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Consultant extends Agent {
    private Map<String, Integer> expertiseDB = new HashMap<>();
    private Random random = new Random();

    @Override
    protected void setup() {
        System.out.println("[CONSULTANT] " + getAID().getLocalName() + " iniciando...");
        loadExpertiseDatabase();
        registerService();
        addBehaviour(new ConsultBehaviour());
    }

    private void loadExpertiseDatabase() {
        expertiseDB.put("Mona Lisa", 78000000);
        expertiseDB.put("Noite Estrelada", 120000000);
        expertiseDB.put("O Grito", 119900000);
        expertiseDB.put("Guernica", 89000000);
        expertiseDB.put("A Persistência da Memória", 65000000);
        System.out.println("[CONSULTANT] Base de conhecimento carregada com " + expertiseDB.size() + " obras");
    }

    private void registerService() {
        System.out.println("[CONSULTANT] Registrando serviço...");
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[CONSULTANT] Serviço registrado com sucesso");
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
                MessageTemplate.and(
                    MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION),
                    MessageTemplate.MatchConversationId("consult-*")
                )
            );
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                System.out.println("[CONSULTANT] Consulta recebida de " + msg.getSender().getLocalName());
                System.out.println("[CONSULTANT] Conteúdo: " + msg.getContent());
                
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                
                try {
                    String[] parts = msg.getContent().split(";");
                    String artwork = parts[0];
                    int currentPrice = Integer.parseInt(parts[1]);
                    int budget = Integer.parseInt(parts[2]);
                    
                    System.out.println("[CONSULTANT] Avaliando: " + artwork + 
                                     " | Preço: $" + currentPrice + 
                                     " | Orçamento: $" + budget);
                    
                    // Simular processamento
                    Thread.sleep(1000);
                    
                    int referenceValue = expertiseDB.getOrDefault(artwork, currentPrice);
                    double marketFactor = 0.8 + random.nextDouble() * 0.4;
                    int fairValue = (int)(referenceValue * marketFactor);
                    
                    String recommendation;
                    int suggestedPrice;
                    
                    if (currentPrice < fairValue * 0.7) {
                        recommendation = "EXCELENTE_OPORTUNIDADE";
                        suggestedPrice = Math.min((int)(currentPrice * 1.15), fairValue);
                    } else {
                        recommendation = "PREÇO_RAZOAVEL";
                        suggestedPrice = currentPrice;
                    }
                    
                    suggestedPrice = Math.min(suggestedPrice, budget);
                    suggestedPrice = Math.max(1, suggestedPrice);
                    
                    String response = recommendation + ";" + suggestedPrice + ";" + fairValue + ";" + referenceValue;
                    reply.setContent(response);
                    
                    System.out.println("[CONSULTANT] Enviando resposta: " + response);
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
    }}