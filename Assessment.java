// Assessment.java - Backend simple para D&D Archetype Assessment
// Compilar: javac Assessment.java
// Ejecutar: java Assessment

import java.util.*;
import java.io.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

public class Assessment {
    
    // ===== MODELOS SIMPLES =====
    static class Question {
        int id; String text; String category; int weight;
        Question(int i, String t, String c, int w) { id=i; text=t; category=c; weight=w; }
    }
    
    static class Answer { int questionId; int value; long timestamp; }
    
    static class Role {
        String id, name, category, description, playstyle;
        int score;
        Role(String i, String n, String c, String d, String p) {
            id=i; name=n; category=c; description=d; playstyle=p; score=50;
        }
    }
    
    static class Result {
        String primaryRole, secondaryRole;
        List<Role> topRoles = new ArrayList<>();
        Map<String, Integer> dimensions = new HashMap<>();
        String interpretation;
        double confidence = 0.85;
        long date = System.currentTimeMillis();
    }
    
    // ===== DATOS =====
    static final List<Question> QUESTIONS = Arrays.asList(
        new Question(1, "Prefiero resolver conflictos con fuerza directa.", "combat", 4),
        new Question(2, "Me siento más cómodo atacando desde las sombras.", "combat", 4),
        new Question(3, "Disfruto estudiando estrategias antes de combatir.", "combat", 3),
        new Question(4, "Prefiero evitar el combate si puedo hablar.", "combat", 3),
        new Question(5, "Me fascina el estudio de lo místico.", "magic", 4),
        new Question(6, "Prefiero soluciones prácticas antes que mágicas.", "magic", 3),
        new Question(7, "Me gustaría poder lanzar hechizos poderosos.", "magic", 4),
        new Question(8, "Confío más en mi fuerza física que en la magia.", "magic", 3),
        new Question(9, "Me resulta fácil convencer a otros con palabras.", "social", 4),
        new Question(10, "Prefiero la acción antes que la diplomacia.", "social", 3),
        new Question(11, "Disfruto siendo el centro de atención.", "social", 4),
        new Question(12, "Prefiero pasar desapercibido.", "social", 4),
        new Question(13, "Creo que el fin justifica los medios.", "moral", 4),
        new Question(14, "Creo que los medios deben ser siempre justos.", "moral", 4),
        new Question(15, "Sigo un código personal de honor estricto.", "moral", 4),
        new Question(16, "Prefiero adaptar mis principios a cada situación.", "moral", 3),
        new Question(17, "Me atrae explorar lugares desconocidos.", "exploration", 4),
        new Question(18, "Prefiero la seguridad de lugares conocidos.", "exploration", 3),
        new Question(19, "Disfruto descifrando mapas y pistas antiguas.", "exploration", 4),
        new Question(20, "Me gusta recolectar tesoros.", "exploration", 4)
        // ... añade más preguntas hasta 120 siguiendo el mismo patrón
    );
    
    static final List<Role> ROLES = Arrays.asList(
        new Role("fighter", "Guerrero/a", "Martial", 
            "Maestro del combate. Resistente, disciplinado y letal.", "Tanque/Daño físico"),
        new Role("rogue", "Pícaro/a", "Stealth", 
            "Experto en sigilo y ataques precisos. Astucia sobre fuerza.", "Daño burst/Utilidad"),
        new Role("wizard", "Mago/a", "Arcane", 
            "Estudioso de lo arcano. Manipula la realidad con hechizos.", "Control/Daño mágico"),
        new Role("cleric", "Clérigo/a", "Divine", 
            "Servidor de una deidad. Sana, protege y guía con fe.", "Soporte/Sanación"),
        new Role("paladin", "Paladín", "Divine Martial", 
            "Guerrero sagrado. Combina fuerza marcial con poder divino.", "Tanque/Daño sagrado"),
        new Role("ranger", "Explorador/a", "Martial Nature", 
            "Maestro de supervivencia y combate a distancia.", "Daño a distancia/Utilidad"),
        new Role("barbarian", "Bárbaro/a", "Primal Martial", 
            "Fuerza bruta y furia. Ignora el dolor, aplasta enemigos.", "Daño masivo/Tanque"),
        new Role("bard", "Bardo/a", "Arcane Social", 
            "Artista hechicero. Usa música y magia para inspirar.", "Soporte/Control social"),
        new Role("druid", "Druida", "Nature Arcane", 
            "Guardián del equilibrio natural. Comanda magia y bestias.", "Versatilidad/Control"),
        new Role("warlock", "Brujo/a", "Pact Arcane", 
            "Pactó con entidad poderosa. Magia única a cambio de lealtad.", "Daño mágico/Utilidad"),
        new Role("monk", "Monje/a", "Martial Spiritual", 
            "Maestro del cuerpo y mente. Combate sin armadura con ki.", "Movilidad/Control"),
        new Role("sorcerer", "Hechicero/a", "Innate Arcane", 
            "Magia fluye de su sangre. Poder innato y espontáneo.", "Daño mágico/Flexibilidad"),
        new Role("artificer", "Artífice", "Arcane Tech", 
            "Ingeniero mágico. Combina magia y tecnología.", "Utilidad/Soporte técnico")
    );
    
    // ===== LÓGICA PRINCIPAL =====
    public static Result calculate(List<Answer> answers) {
        Result r = new Result();
        
        // 1. Calcular puntuaciones por dimensión
        Map<String, Integer> dimRaw = new HashMap<>();
        Map<String, Integer> dimWeight = new HashMap<>();
        
        for (Answer a : answers) {
            Question q = QUESTIONS.stream().filter(x -> x.id == a.questionId).findFirst().orElse(null);
            if (q == null || q.category.equals("validity")) continue;
            dimRaw.put(q.category, dimRaw.getOrDefault(q.category, 0) + a.value * q.weight);
            dimWeight.put(q.category, dimWeight.getOrDefault(q.category, 0) + q.weight);
        }
        
        // Normalizar a escala T (20-80)
        for (String cat : dimRaw.keySet()) {
            double avg = (double) dimRaw.get(cat) / dimWeight.get(cat);
            int tScore = Math.min(80, Math.max(20, (int)(40 + (avg - 3) * 10)));
            r.dimensions.put(cat, tScore);
        }
        
        // 2. Calcular afinidad con roles
        for (Role role : ROLES) {
            int score = 50;
            switch(role.id) {
                case "fighter": score = (r.dimensions.getOrDefault("combat",50)) + 10; break;
                case "rogue": score = (int)((r.dimensions.getOrDefault("combat",50)*0.7) + (r.dimensions.getOrDefault("social",50)*0.8)); break;
                case "wizard": score = (r.dimensions.getOrDefault("magic",50)) + (r.dimensions.getOrDefault("exploration",50)/2); break;
                case "cleric": score = (int)((r.dimensions.getOrDefault("magic",50)*0.8) + (r.dimensions.getOrDefault("social",50)*0.7)); break;
                case "paladin": score = (r.dimensions.getOrDefault("combat",50)) + (r.dimensions.getOrDefault("moral",50)/2); break;
                case "ranger": score = (r.dimensions.getOrDefault("exploration",50)) + (r.dimensions.getOrDefault("combat",50)/2); break;
                case "barbarian": score = (r.dimensions.getOrDefault("combat",50)) + (r.dimensions.getOrDefault("moral",50)<45?15:0); break;
                case "bard": score = (r.dimensions.getOrDefault("social",50)) + (r.dimensions.getOrDefault("magic",50)/2); break;
                case "druid": score = (int)((r.dimensions.getOrDefault("magic",50)*0.8) + (r.dimensions.getOrDefault("moral",50)>50?10:0)); break;
                case "warlock": score = (r.dimensions.getOrDefault("magic",50)) + (r.dimensions.getOrDefault("moral",50)<50?10:0); break;
                case "monk": score = (int)((r.dimensions.getOrDefault("combat",50)*0.7) + (r.dimensions.getOrDefault("moral",50)*0.6)); break;
                case "sorcerer": score = (r.dimensions.getOrDefault("magic",50)) + (r.dimensions.getOrDefault("combat",50)/3); break;
                case "artificer": score = (int)((r.dimensions.getOrDefault("magic",50)*0.6) + (r.dimensions.getOrDefault("exploration",50)*0.7)); break;
            }
            role.score = Math.min(100, Math.max(0, score));
        }
        
        // 3. Ordenar roles por puntuación
        ROLES.sort((a,b) -> b.score - a.score);
        r.topRoles = new ArrayList<>(ROLES.subList(0, Math.min(5, ROLES.size())));
        r.primaryRole = r.topRoles.get(0).name;
        r.secondaryRole = r.topRoles.size() > 1 ? r.topRoles.get(1).name : "N/A";
        
        // 4. Generar interpretación simple
        r.interpretation = String.format(
            "╔════════════════════════════╗\n" +
            "║  ARQUETIPO: %-14s  ║\n" +
            "╚════════════════════════════╝\n\n" +
            "🎭 Rol Principal: %s\n" +
            "📊 Categoría: %s\n" +
            "⚔️ Estilo: %s\n\n" +
            "📈 Dimensiones:\n%s\n\n" +
            "💡 Consejo: ¡Juega como te divierta!\n",
            r.primaryRole.toUpperCase(),
            r.primaryRole,
            r.topRoles.get(0).category,
            r.topRoles.get(0).playstyle,
            buildDimensions(r.dimensions)
        );
        
        return r;
    }
    
    static String buildDimensions(Map<String, Integer> dims) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : dims.entrySet()) {
            String level = e.getValue() < 40 ? "Bajo" : e.getValue() > 60 ? "Alto" : "Promedio";
            sb.append(String.format("  • %s: %dT (%s)\n", e.getKey(), e.getValue(), level));
        }
        return sb.toString();
    }
    
    // ===== SERVIDOR HTTP SIMPLE =====
    public static void startServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/api/calculate", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                // Parseo JSON muy básico (para producción usa Gson/Jackson)
                List<Answer> answers = parseAnswers(body);
                Result result = calculate(answers);
                String json = toJson(result);
                
                exchange.sendResponseHeaders(200, json.length());
                exchange.getResponseBody().write(json.getBytes());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        
        server.createContext("/api/roles", exchange -> {
            String json = toJson(ROLES);
            exchange.sendResponseHeaders(200, json.length());
            exchange.getResponseBody().write(json.getBytes());
            exchange.close();
        });
        
        server.setExecutor(null);
        server.start();
        System.out.println("🎲 Servidor D&D Assessment iniciado en http://localhost:" + port);
    }
    
    // ===== UTILS MUY BÁSICOS (sin librerías externas) =====
    static List<Answer> parseAnswers(String json) {
        // Implementación mínima - en producción usar Gson
        List<Answer> list = new ArrayList<>();
        // ... parsing manual simple o delegar a frontend
        return list;
    }
    
    static String toJson(Object obj) {
        // Implementación mínima - en producción usar Gson
        if (obj instanceof Result) {
            Result r = (Result) obj;
            return String.format(
                "{\"primaryRole\":\"%s\",\"secondaryRole\":\"%s\",\"confidence\":%.2f,\"date\":%d}",
                r.primaryRole, r.secondaryRole, r.confidence, r.date
            );
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            for (Role role : ROLES) {
                sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"score\":%d},",
                    role.id, role.name, role.score
                ));
            }
            if (sb.length() > 1) sb.setLength(sb.length() - 1);
            sb.append("]");
            return sb.toString();
        }
        return "{}";
    }
    
    // ===== MAIN =====
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        startServer(port);
    }
}
