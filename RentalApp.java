import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate; // TIME API
import java.time.temporal.ChronoUnit; // TIME API
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RentalApp {
    private static ArrayList<Vehicle> fleet = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Seeding the fleet with 8 distinct vehicles
        fleet.add(new Vehicle("C001", "Tesla Model 3", 75.0));
        fleet.add(new Vehicle("C002", "Toyota RAV4 (SUV)", 55.0));
        fleet.add(new Vehicle("C003", "Honda Civic (Economy)", 40.0));
        fleet.add(new Vehicle("C004", "Ford Mustang (Sport)", 90.0));
        fleet.add(new Vehicle("C005", "BMW X5 (Luxury SUV)", 120.0));
        fleet.add(new Vehicle("S001", "Vespa Primavera Scooter", 25.0));
        fleet.add(new Vehicle("S002", "Yamaha NMAX Scooter", 30.0));
        fleet.add(new Vehicle("B001", "Specialized E-Bike", 15.0));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new DashboardHandler());
        server.setExecutor(null); 
        server.start();

        System.out.println("🚀 Server started successfully!");
        System.out.println("🔗 Open your browser and go to: http://localhost:8080");
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("GET")) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);

                if (params.containsKey("action")) {
                    String action = params.get("action");
                    String id = params.get("id");
                    Vehicle v = findVehicle(id);

                    if (v != null) {
                        if (action.equals("rent")) {
                            v.rentItem();
                            // Automatically stamp the rental start date as TODAY using Time API
                            v.setRentalStartDate(LocalDate.now());
                            
                            String pay = params.getOrDefault("pay", "later");
                            if (pay.equals("now")) {
                                v.setPaymentStatus(PaymentStatus.PAID);
                                v.setAmountDue(0.0);
                            } else {
                                v.setPaymentStatus(PaymentStatus.PENDING);
                                // Set an estimated amount due based on a standard 3-day baseline
                                v.setAmountDue(v.calculateTotalCost(3)); 
                            }
                        } else if (action.equals("return")) {
                            // TIME API: Mocking a return date that happens 4 days in the future to show it works
                            LocalDate returnDate = LocalDate.now().plusDays(4); 
                            
                            // TIME API: Calculate exact days between start and return date
                            long daysRented = ChronoUnit.DAYS.between(v.getRentalStartDate(), returnDate);
                            if (daysRented <= 0) daysRented = 1; // Minimum 1-day charge

                            double finalCost = v.calculateTotalCost((int) daysRented);
                            
                            v.returnItem();
                            v.setPaymentStatus(PaymentStatus.NONE);
                            v.setAmountDue(0.0);
                            v.setRentalStartDate(null);
                        }
                    }
                }
            }

            // Generate raw text/HTML view for the browser console layout
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family:monospace; padding:20px; background-color:#1e1e1e; color:#d4d4d4;'>");
            html.append("<h2>=== VEHICLE RENTAL WEB DASHBOARD ===</h2>");
            html.append("<p>Tracking vehicle operations with Java Time and Stream APIs.</p><br/>");
            
            html.append("<table border='1' style='border-collapse:collapse; cellpadding:10px; border-color:#444;'>");
            html.append("<tr style='background-color:#333;'><th>ID</th><th>Model</th><th>Rate/Day</th><th>Status</th><th>Rental Start</th><th>Payment</th><th>Dues</th><th>Actions</th></tr>");
            
            for (Vehicle v : fleet) {
                html.append("<tr>");
                html.append("<td>").append(v.getId()).append("</td>");
                html.append("<td>").append(v.getModel()).append("</td>");
                html.append("<td>$").append(v.getRatePerDay()).append("</td>");
                html.append("<td>").append(v.getStatus()).append("</td>");
                html.append("<td>").append(v.getRentalStartDate() != null ? v.getRentalStartDate() : "N/A").append("</td>");
                html.append("<td>").append(v.getPaymentStatus()).append("</td>");
                html.append("<td>$").append(v.getAmountDue()).append("</td>");
                html.append("<td>");
                if (v.getStatus() == VehicleStatus.AVAILABLE) {
                    html.append("<a href='/?action=rent&id=").append(v.getId()).append("&pay=now' style='color:#4fc1ff;'>[Rent & Pay Now]</a> ");
                    html.append("<a href='/?action=rent&id=").append(v.getId()).append("&pay=later' style='color:#ce9178;'>[Rent & Pay Later]</a>");
                } else {
                    html.append("<a href='/?action=return&id=").append(v.getId()).append("' style='color:#4fc1ff;'>[Process Return (Simulate 4 Days)]</a>");
                }
                html.append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
            html.append("</body></html>");

            String response = html.toString();
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } else {
                    result.put(pair[0], "");
                }
            }
            return result;
        }
    }

    // STREAM API: Uses high-level functional architecture to search the list elegantly
    private static Vehicle findVehicle(String id) {
        return fleet.stream()
                    .filter(v -> v.getId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);
    }
}

// =========================================================================
// DATA STRUCTURES
// =========================================================================
interface Rentable {
    void rentItem();
    void returnItem();
}

enum VehicleStatus { AVAILABLE, RENTED }
enum PaymentStatus { PAID, PENDING, NONE }

class Vehicle implements Rentable {
    private String id;
    private String model;
    private double ratePerDay;
    private VehicleStatus status; 
    private PaymentStatus paymentStatus; 
    private double amountDue;
    private LocalDate rentalStartDate; // TRACKS REAL DATES

    public Vehicle(String id, String model, double ratePerDay) {
        this.id = id;
        this.model = model;
        this.ratePerDay = ratePerDay;
        this.status = VehicleStatus.AVAILABLE;
        this.paymentStatus = PaymentStatus.NONE;
        this.amountDue = 0.0;
        this.rentalStartDate = null;
    }

    public String getId() { return id; }
    public String getModel() { return model; }
    public double getRatePerDay() { return ratePerDay; }
    public VehicleStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public double getAmountDue() { return amountDue; }
    public void setAmountDue(double amountDue) { this.amountDue = amountDue; }
    public LocalDate getRentalStartDate() { return rentalStartDate; }
    public void setRentalStartDate(LocalDate date) { this.rentalStartDate = date; }

    @Override public void rentItem() { this.status = VehicleStatus.RENTED; }
    @Override public void returnItem() { this.status = VehicleStatus.AVAILABLE; }
    public double calculateTotalCost(int days) { return this.ratePerDay * days; }
}
