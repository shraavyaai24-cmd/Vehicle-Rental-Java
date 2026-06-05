import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RentalApp {

    // Vehicle Data Model
    static class Vehicle {
        String id;
        String model;
        double rate;
        String status;
        String rentedByUserId; // Tracks which user currently has it

        public Vehicle(String id, String model, double rate) {
            this.id = id;
            this.model = model;
            this.rate = rate;
            this.status = "AVAILABLE";
            this.rentedByUserId = "NONE";
        }
    }

    // User Data Model 
    static class User {
        String id;
        String name;
        double dues;

        public User(String id, String name) {
            this.id = id;
            this.name = name;
            this.dues = 0.0;
        }
    }

    private static List<Vehicle> fleet = new ArrayList<>();
    private static List<User> users = new ArrayList<>();
    
    // Simulating active session user profile "Rahul Sharma"
    private static final String CURRENT_LOGGED_IN_USER_ID = "U001";

    public static void main(String[] args) throws IOException {
        // Seed initial Users
        users.add(new User("U001", "Rahul Sharma"));
        users.add(new User("U002", "Priya Patel"));

        // Seed initial Fleet Status
        fleet.add(new Vehicle("C001", "Tesla Model 3", 6000.0));
        fleet.add(new Vehicle("C002", "Toyota RAV4 (SUV)", 4500.0));
        fleet.add(new Vehicle("C003", "Honda Civic (Economy)", 3000.0));
        fleet.add(new Vehicle("C004", "Ford Mustang (Sport)", 8000.0));
        fleet.add(new Vehicle("C005", "BMW X5 (Luxury SUV)", 10000.0));
        fleet.add(new Vehicle("S001", "Vespa Primavera Scooter", 800.0));
        fleet.add(new Vehicle("S002", "Yamaha NMAX Scooter", 1200.0));
        fleet.add(new Vehicle("B001", "Specialized E-Bike", 500.0));

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new DashboardHandler());
        server.setExecutor(null);
        System.out.println("Rental Application operational with Security Guards on port: " + port);
        server.start();
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }

            String action = params.get("action");
            String vehicleId = params.get("id");
            String alertMessage = "";

            // Query active user state
            User currentUser = users.stream()
                    .filter(u -> u.id.equals(CURRENT_LOGGED_IN_USER_ID))
                    .findFirst()
                    .orElse(null);

            if (action != null && vehicleId != null && currentUser != null) {
                for (Vehicle v : fleet) {
                    if (v.id.equals(vehicleId)) {
                        
                        // 1. BACKEND CHECKPOINT GUARD: Block rentals if user has an unpaid balance
                        if (action.equals("rent_now") || action.equals("rent_later")) {
                            if (currentUser.dues > 0) {
                                alertMessage = "<div class='alert alert-danger d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                        + "    <i class='bi bi-exclamation-octagon-fill me-2 fs-5'></i>"
                                        + "    <div><strong>Transaction Blocked!</strong> Clear your current balance of ₹" + currentUser.dues + " before opening a new booking contract.</div>"
                                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                        + "</div>";
                            } else {
                                // Execute normal processing parameters if dues are zero
                                v.status = "RENTED";
                                v.rentedByUserId = currentUser.id;
                                
                                if (action.equals("rent_now")) {
                                    currentUser.dues = 0.0;
                                    alertMessage = "<div class='alert alert-success d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                            + "    <i class='bi bi-check-circle-fill me-2 fs-5'></i>"
                                            + "    <div><strong>Success!</strong> Booking completed for " + v.model + " (Advance Settled).</div>"
                                            + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                            + "</div>";
                                } else {
                                    currentUser.dues = v.rate; // Apply liability debt balance
                                    alertMessage = "<div class='alert alert-warning d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                            + "    <i class='bi bi-exclamation-triangle-fill me-2 fs-5'></i>"
                                            + "    <div><strong>Notice:</strong> " + v.model + " checked out. Booking invoice of ₹" + currentUser.dues + " loaded to your profile.</div>"
                                            + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                            + "</div>";
                                }
                            }
                        } 
                        
                        // Handle Returns
                        else if (action.equals("return")) {
                            if (v.rentedByUserId.equals(currentUser.id)) {
                                v.status = "AVAILABLE";
                                v.rentedByUserId = "NONE";
                                alertMessage = "<div class='alert alert-info d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                        + "    <i class='bi bi-info-circle-fill me-2 fs-5'></i>"
                                        + "    <div><strong>Vehicle Returned:</strong> " + v.model + " is checked back into base registry. Please clear your balance below if unpaid.</div>"
                                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                        + "</div>";
                            }
                        }
                        break;
                    }
                }
            }

            // Settle Account Balance
            if ("clear_dues".equals(action) && currentUser != null) {
                currentUser.dues = 0.0;
                alertMessage = "<div class='alert alert-success d-flex align-items-center alert-dismissible fade show' role='alert'>"
                        + "    <i class='bi bi-shield-check me-2 fs-5'></i>"
                        + "    <div><strong>Account Settle Active!</strong> Balance verified. Your profile locks are lifted.</div>"
                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                        + "</div>";
            }

            // Render Document Output
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<title>Enterprise Fleet Operator</title>")
                .append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>")
                .append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css'>")
                .append("<style>body { background-color: #f4f6f9; font-family: 'Segoe UI', system-ui, sans-serif; } .card-shadow { box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.05); border: none; border-radius: 12px; }</style>")
                .append("</head><body>");
                
                // Header Profiling Bar
                html.append("<nav class='navbar navbar-expand-lg navbar-dark bg-dark py-3 mb-4 shadow-sm'>")
                .append("    <div class='container'>")
                .append("        <a class='navbar-brand fw-bold text-white' href='/'><i class='bi bi-car-front-fill me-2 text-warning'></i>VEHICLE MANAGEMENT APP</a>")
                .append("        <span class='navbar-text text-white'><i class='bi bi-shield-lock-fill text-warning me-1'></i> Portal Operator: <strong class='text-info'>").append(currentUser.name).append("</strong></span>")
                .append("    </div>")
                .append("</nav>");
                
                html.append("<div class='container'>").append(alertMessage)

                // Financial Control Board Layout Widgets
                .append("    <div class='row g-3 mb-4'>")
                .append("        <div class='col-md-6'>")
                .append("            <div class='card card-shadow bg-white p-3 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted small mb-1'>Rental Clearance Checks</h6>")
                .append(currentUser.dues > 0 ? "<h4 class='fw-bold text-danger mb-0'><i class='bi bi-lock-fill me-1'></i> PROFILE SUSPENDED</h4>" : "<h4 class='fw-bold text-success mb-0'><i class='bi bi-unlock-fill me-1'></i> SYSTEM CLEAR</h4>").append("</div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-6'>")
                .append("            <div class='card card-shadow bg-white p-3 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted small mb-1'>Current Profile Liability Balance</h6><h3 class='fw-bold mb-0 text-danger'>₹").append(currentUser.dues).append("</h3></div>")
                .append("                <div>").append(currentUser.dues > 0 ? "<a href='/?action=clear_dues' class='btn btn-success btn-sm fw-bold'><i class='bi bi-currency-rupee me-1'></i> Clear Debt Now</a>" : "").append("</div>")
                .append("            </div>")
                .append("        </div>")
                .append("    </div>")

                // Main Fleet Table Module Registry
                .append("    <div class='card card-shadow p-4 bg-white mb-5'>")
                .append("        <h4 class='fw-bold mb-4 text-dark text-uppercase fs-5 letter-spacing'>Core Fleet Allocation Registry</h4>")
                .append("        <div class='table-responsive'>")
                .append("            <table class='table table-hover align-middle mb-0'>")
                .append("                <thead class='table-light text-secondary small'>")
                .append("                    <tr><th>ID</th><th>Vehicle Specifications</th><th>Base Tariff Rate</th><th>Status</th><th>Current Renter</th><th class='text-end'>Available Directives</th></tr>")
                .append("                </thead><tbody>");

            // Loop rendering logic block
            for (Vehicle v : fleet) {
                String badgeClass = v.status.equals("AVAILABLE") ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger";
                String renterText = v.rentedByUserId.equals(currentUser.id) ? "You" : (v.rentedByUserId.equals("NONE") ? "N/A" : v.rentedByUserId);
                
                html.append("                    <tr>")
                    .append("                        <td><span class='fw-bold text-secondary'>").append(v.id).append("</span></td>")
                    .append("                        <td><span class='fw-semibold text-dark'>").append(v.model).append("</span></td>")
                    .append("                        <td>₹").append(v.rate).append("/day</td>")
                    .append("                        <td><span class='badge ").append(badgeClass).append(" px-2.5 py-1.5'>").append(v.status).append("</span></td>")
                    .append("                        <td class='text-muted small'>").append(renterText).append("</td>")
                    .append("                        <td class='text-end'>");
                
                if (v.status.equals("AVAILABLE")) {
                    // 2. VISUAL FRONTEND BLOCK: If user has dues, append "disabled" attribute to buttons so they can't click them
                    if (currentUser.dues > 0) {
                        html.append("                            <button class='btn btn-sm btn-secondary opacity-50 me-1' disabled><i class='bi bi-lock-fill me-1'></i> Locked</button>")
                            .append("                            <button class='btn btn-sm btn-outline-secondary opacity-50' disabled><i class='bi bi-lock-fill me-1'></i> Locked</button>");
                    } else {
                        html.append("                            <a href='/?action=rent_now&id=").append(v.id).append("' class='btn btn-sm btn-primary'><i class='bi bi-wallet2 me-1'></i> Rent Now</a>")
                            .append("                            <a href='/?action=rent_later&id=").append(v.id).append("' class='btn btn-sm btn-outline-secondary'><i class='bi bi-clock-history me-1'></i> Pay Later</a>");
                    }
                } else if (v.rentedByUserId.equals(currentUser.id)) {
                    html.append("                            <a href='/?action=return&id=").append(v.id).append("' class='btn btn-sm btn-danger'><i class='bi bi-arrow-left-right me-1'></i> Terminate & Return</a>");
                } else {
                    html.append("                            <button class='btn btn-sm btn-light text-muted' disabled>Occupied</button>");
                }
                html.append("                        </td></tr>");
            }

            html.append("                </tbody></table></div></div></div>")
                .append("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js'></script>")
                .append("</body></html>");

            byte[] responseBytes = html.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
