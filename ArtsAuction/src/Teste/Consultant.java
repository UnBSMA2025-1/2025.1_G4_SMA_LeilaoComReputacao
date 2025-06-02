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
    // Gerador de números aleatórios para variação de preços
    private Random random = new Random();
    
    // Total de ganhos acumulados pelo consultor
    private int earnings = 0;

    @Override
    protected void setup() {
        // Mensagem de inicialização do agente
        System.out.println("[CONSULTANT] " + getAID().getLocalName() + " iniciando...");
        
        // Registra o serviço no Directory Facilitator (DF)
        registerService();
        
        // Adiciona o comportamento principal do consultor
        addBehaviour(new ConsultBehaviour());
    }

    // Método para registrar o serviço do consultor no DF
    private void registerService() {
        try {
            // Cria a descrição do agente para registro
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            
            // Cria a descrição do serviço oferecido
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);  // Tipo do serviço
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);     // Nome do serviço
            dfd.addServices(sd);
            
            // Registra o serviço no DF
            DFService.register(this, dfd);
            System.out.println("[CONSULTANT] Serviço registrado com sucesso | Taxa de consulta: $" + ArtConstants.CONSULTATION_FEE);
        } catch (FIPAException fe) {
            System.err.println("[CONSULTANT] Falha no registro: " + fe.getMessage());
            doDelete();  // Encerra o agente em caso de falha
        }
    }

    // Comportamento principal do consultor para lidar com requisições
    private class ConsultBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Template para filtrar apenas mensagens REQUEST com a ontologia correta
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            // Recebe a mensagem (se houver)
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                // Incrementa os ganhos com a taxa de consulta
                earnings += ArtConstants.CONSULTATION_FEE;
                System.out.println("[CONSULTANT] Consulta recebida de " + msg.getSender().getLocalName() + 
                                 " | Taxa cobrada: $" + ArtConstants.CONSULTATION_FEE + 
                                 " | Total ganho: $" + earnings);
                
                // Prepara a resposta
                ACLMessage reply = msg.createReply();
                
                try {
                    // Processa o conteúdo da mensagem
                    String[] parts = msg.getContent().split(";");
                    String artwork = parts[0];           // Nome da obra de arte
                    int currentPrice = Integer.parseInt(parts[1]);  // Preço atual
                    int budget = Integer.parseInt(parts[2]);        // Orçamento do comprador
                    
                    // Simula tempo de processamento (1-3 segundos)
                    Thread.sleep(1000 + random.nextInt(2000));
                    
                    // Calcula preço sugerido com variação aleatória (80-100% do preço atual)
                    double variation = 0.80 + random.nextDouble() * 0.2;
                    int suggestedPrice = (int)(currentPrice * variation);
                    
                    // Garante que o preço sugerido não ultrapasse o orçamento
                    suggestedPrice = Math.min(suggestedPrice, budget);
                    
                    // Prepara a resposta
                    String response = suggestedPrice + ";" + variation;
                    System.out.println("[CONSULTANT] Sugestão enviada: " + response);
                    
                    // Configura a resposta
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(response);
                } catch (Exception e) {
                    // Em caso de erro, envia mensagem de falha
                    System.err.println("[CONSULTANT] Erro ao processar consulta: " + e.getMessage());
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Erro: " + e.getMessage());
                }
                
                // Envia a resposta
                send(reply);
            } else {
                // Bloqueia o comportamento até receber nova mensagem
                block();
            }
        }
    }
}