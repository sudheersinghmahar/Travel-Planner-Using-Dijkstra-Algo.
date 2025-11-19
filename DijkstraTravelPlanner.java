import java.io.*;
import java.net.*;
import java.util.*;

public class DijkstraTravelPlanner {

    private static Map<String, Map<String, Integer>> cityGraph = new HashMap<>();

    public static void main(String[] args) throws Exception {
        initializeGraph();

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        System.out.println("Server running on http://localhost:8000");

        server.createContext("/route", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {

                Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
                String source = query.get("source");
                String destination = query.get("destination");

                Result result = dijkstra(source, destination);

                String json = "{"
                        + "\"distance\": \"" + result.distance + "\","
                        + "\"path\": \"" + String.join(" → ", result.path) + "\""
                        + "}";

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length());
                OutputStream os = exchange.getResponseBody();
                os.write(json.getBytes());
                os.close();
            }
        });

        server.start();
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) map.put(pair[0], pair[1]);
        }
        return map;
    }

    private static void initializeGraph() {
        addEdge("Delhi", "Agra", 233);
        addEdge("Delhi", "Jaipur", 280);
        addEdge("Delhi", "Dehradun", 250);
        addEdge("Delhi", "Lucknow", 555);
        addEdge("Delhi", "Chandigarh", 245);

        addEdge("Agra", "Jaipur", 240);
        addEdge("Agra", "Gwalior", 120);

        addEdge("Jaipur", "Udaipur", 395);

        addEdge("Dehradun", "Mussoorie", 35);
        addEdge("Dehradun", "Rishikesh", 45);
        addEdge("Dehradun", "Dharamshala", 240);

        addEdge("Mussoorie", "Dehradun", 35);

        addEdge("Udaipur", "Ahmedabad", 260);

        addEdge("Ahmedabad", "Mumbai", 525);
        addEdge("Ahmedabad", "Surat", 280);

        addEdge("Mumbai", "Pune", 150);
        addEdge("Mumbai", "Goa", 590);

        addEdge("Pune", "Hyderabad", 560);

        addEdge("Hyderabad", "Bangalore", 570);
        addEdge("Hyderabad", "Telangana", 0);

        addEdge("Bangalore", "Chennai", 350);
        addEdge("Bangalore", "Mysuru", 150);

        addEdge("Chennai", "Pondicherry", 160);
        addEdge("Chennai", "Tamil Nadu", 0);

        addEdge("Rishikesh", "Dehradun", 45);

        addEdge("Chandigarh", "Shimla", 115);

        addEdge("Shimla", "Kullu", 210);

        addEdge("Kullu", "Manali", 40);

        addEdge("Lucknow", "Varanasi", 320);

        addEdge("Varanasi", "Patna", 250);

        addEdge("Bhimtal", "Nainital", 22);

        addEdge("Nainital", "Haldwani", 40);

        addEdge("Indore", "Bhopal", 190);

        addEdge("Pithoragarh", "Champawat", 75);

        addEdge("Champawat", "Ranikhet", 90);

        addEdge("Gwalior", "Agra", 120);

        addEdge("Amritsar", "Jammu", 210);
        addEdge("Amritsar", "Chandigarh", 230);

        addEdge("Jammu", "Srinagar", 295);

        addEdge("Srinagar", "Gulmarg", 50);

        addEdge("Gangtok", "Silguri", 115);

        addEdge("Guwahati", "Shillong", 100);

        addEdge("Kochi", "Munnar", 130);

        addEdge("Madurai", "Kanyakumari", 245);

        addEdge("Nagpur", "Raipur", 290);

        addEdge("Rameswaram", "Tirupati", 400);

        addEdge("Panaji", "Goa", 0);

        addEdge("Daman", "Mumbai", 170);

        addEdge("Diu", "Surat", 225);

        addEdge("Surat", "Ahmedabad", 280);
        addEdge("Surat", "Diu", 225);

        addSymmetricEdges();
    }

    private static void addEdge(String from, String to, int distance) {
        cityGraph.putIfAbsent(from, new HashMap<>());
        cityGraph.get(from).put(to, distance);
    }

    private static void addSymmetricEdges() {
        for (String city : new HashSet<>(cityGraph.keySet())) {
            for (Map.Entry<String, Integer> entry : cityGraph.get(city).entrySet()) {
                String neighbor = entry.getKey();
                int dist = entry.getValue();
                cityGraph.putIfAbsent(neighbor, new HashMap<>());
                cityGraph.get(neighbor).putIfAbsent(city, dist);
            }
        }
    }

    private static Result dijkstra(String start, String end) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> unvisited = new HashSet<>(cityGraph.keySet());

        for (String city : cityGraph.keySet()) {
            distances.put(city, Integer.MAX_VALUE);
            previous.put(city, null);
        }

        distances.put(start, 0);

        while (!unvisited.isEmpty()) {
            String current = null;
            int smallestDist = Integer.MAX_VALUE;

            for (String city : unvisited) {
                int dist = distances.get(city);
                if (dist < smallestDist) {
                    smallestDist = dist;
                    current = city;
                }
            }

            if (current == null || current.equals(end)) break;

            unvisited.remove(current);

            for (Map.Entry<String, Integer> entry : cityGraph.get(current).entrySet()) {
                String neighbor = entry.getKey();
                int dist = entry.getValue();

                if (!unvisited.contains(neighbor)) continue;

                int newDist = distances.get(current) + dist;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                }
            }
        }

        List<String> path = new ArrayList<>();
        String step = end;

        while (step != null) {
            path.add(step);
            step = previous.get(step);
        }

        Collections.reverse(path);

        if (distances.get(end) == Integer.MAX_VALUE) path.clear();

        return new Result(distances.get(end), path);
    }

    static class Result {
        int distance;
        List<String> path;

        Result(int distance, List<String> path) {
            this.distance = distance;
            this.path = path;
        }
    }
}
