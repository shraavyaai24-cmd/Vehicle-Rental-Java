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

    // Simple inner class to hold Vehicle Data
    static class Vehicle {
        String id;
        String model;
        double rate;
        String status;
        String rentalStart;
        String payment;
        double dues;

        public Vehicle(String id, String model, double rate) {
            this.id = id;
            this.model = model;
            this.rate = rate;
            this.status = "AVAILABLE";
            this.rentalStart = "N/A";
            this.payment = "NONE";
            this.dues = 0.0;
        }
    }

    private static List<Vehicle> fleet = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Initialize the default fleet with localized Rupee daily rates
        fleet.add(new Vehicle("C001", "Tesla Model 3", 6000.0));
        fleet.add(new Vehicle("C002", "Toyota RAV4 (SUV)", 4500.0));
        fleet.add(new Vehicle("C003", "Honda Civic (Economy)", 3000.0));
        fleet.add(new Vehicle("C004", "Ford Mustang (Sport)", 8000.0));
        fleet.add(new Vehicle("C005", "BMW X5 (Luxury SUV)", 10000.0));
        fleet.add(new Vehicle("S001", "Vespa Primavera Scooter", 800.0));
        fleet.add(new Vehicle("S002", "Yamaha NMAX Scooter", 1200.0));
        fleet.add(new Vehicle("B001", "Specialized E-Bike", 500.0));

        // Render sets a PORT environment variable dynamically. Default to 8080.
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new DashboardHandler());
        server.setExecutor(null);
        System.out.println("Server listening smoothly on port: " + port);
        server.start();
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI requestedUri = exchange.getRequestURI();
            String query = requestedUri.getRawQuery();
            
            // Simple Query Parsing Logic
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }

            // Handle dashboard state updates based on user interaction
            String action = params.get("action");
            String id = params.get("id");
            String alertMessage = "";

            if (action != null && id != null) {
                for (Vehicle v : fleet) {
                    if (v.id.equals(id)) {
                        if (action.equals("rent_now")) {
                            v.status = "RENTED";
                            v.rentalStart = "Just Now";
                            v.payment = "PAID";
                            v.dues = 0.0;
                            alertMessage = "<div class='alert alert-success alert-dismissible fade show' role='alert'><strong>Success!</strong> Successfully rented " + v.model + " (Paid Advance).<button type='button' class='btn-close' data-bs-dismiss='alert'></button></div>";
                        } else if (action.equals("rent_later")) {
                            v.status = "RENTED";
                            v.rentalStart = "Just Now";
                            v.payment = "PENDING";
                            v.dues = v.rate;
                            alertMessage = "<div class='alert alert-warning alert-dismissible fade show' role='alert'><strong>Reserved!</strong> " + v.model + " rented with pending payment of ₹" + v.dues + ".<button type='button' class='btn-close' data-bs-dismiss='alert'></button></div>";
                        } else if (action.equals("return")) {
                            v.status = "AVAILABLE";
                            v.rentalStart = "N/A";
                            v.payment = "NONE";
                            v.dues = 0.0;
                            alertMessage = "<div class='alert alert-info alert-dismissible fade show' role='alert'><strong>Returned!</strong> " + v.model + " is now back in the fleet array.<button type='button' class='btn-close' data-bs-dismiss='alert'></button></div>";
                        }
                        break;
                    }
                }
            }

            // Build Beautiful HTML Response String
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>")
                .append("<html lang='en'>")
                .append("<head>")
                .append("    <meta charset='UTF-8'>")
                .append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("    <title>Vehicle Rental Hub</title>")
                .append("    <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>")
                .append("    <link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css'>")
                .append("    <style>")
                .append("        body { background-color: #f4f6f9; font-family: 'Segoe UI', Roboto, sans-serif; }")
                .append("        .navbar-brand { font-weight: 700; letter-spacing: 0.5px; }")
                .append("        .card-shadow { box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.05); border: none; border-radius: 12px; }")
                .append("        .table th { font-weight: 600; text-transform: uppercase; font-size: 0.8rem; letter-spacing: 0.5px; color: #6c757d; }")
                .append("        .btn-action { font-weight: 500; border-radius: 6px; padding: 0.4rem 0.8rem; font-size: 0.85rem; }")
                .append("    </style>")
                .append("</head>")
                .append("<body>")
                
                // Navigation Bar
                .append("<nav class='navbar navbar-expand-lg navbar-dark bg-dark py-3 mb-4 shadow-sm'>")
                .append("    <div class='container'>")
                .append("        <a class='navbar-brand' href='/'><i class='bi bi-car-front-fill me-2 text-warning'></i>VEHICLE RENTAL SYSTEM</a>")
                .append("        <span class='navbar-text text-white-50 fs-6 d-none d-md-inline'>Engineered with Java Streams API & Docker</span>")
                .append("    </div>")
                .append("</nav>")
                
                // Main Container
                .append("<div class='container'>")
                .append(alertMessage) // Inject alert notification banner if applicable

                // Stats Dashboard Header Cards
                .append("    <div class='row g-3 mb-4'>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted mb-1'>Total Fleet</h6><h3 class='fw-bold mb-0'>")
                .append(fleet.size()).append("</h3></div>")
                .append("                <div class='fs-1 text-primary'><i class='bi bi-truck'></i></div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted mb-1'>Active Rentals</h6><h3 class='fw-bold mb-0 text-success'>")
                .append(fleet.stream().filter(v -> v.status.equals("RENTED")).count()).append("</h3></div>")
                .append("                <div class='fs-1 text-success'><i class='bi bi-keyframes-rounded'></i></div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted mb-1'>Pending Payments</h6><h3 class='fw-bold mb-0 text-danger'>₹")
                .append(fleet.stream().mapToDouble(v -> v.dues).sum()).append("</h3></div>")
                .append("                <div class='fs-1 text-danger'><i class='bi bi-cash-coin'></i></div>")
                .append("            </div>")
                .append("        </div>")
                .append("    </div>")

                // Main Fleet Table Section
                .append("    <div class='card card-shadow p-4 bg-white mb-5'>")
                .append("        <div class='d-flex justify-content-between align-items-center mb-4'>")
                .append("            <h4 class='fw-bold mb-0 text-dark'>Live Fleet Status Registry</h4>")
                .append("            <a href='/' class='btn btn-outline-dark btn-sm'><i class='bi bi-arrow-clockwise me-1'></i> Refresh Directory</a>")
                .append("        </div>")
                .append("        <div class='table-responsive'>")
                .append("            <table class='table table-hover align-middle mb-0'>")
                .append("                <thead>")
                .append("                    <tr>")
                .append("                        <th>ID</th>")
                .append("                        <th>Model Description</th>")
                .append("                        <th>Rate/Day</th>")
                .append("                        <th>Status</th>")
                .append("                        <th>Rental Window</th>")
                .append("                        <th>Payment Status</th>")
                .append("                        <th>Dues</th>")
                .append("                        <th class='text-end'>Operations / Actions</th>")
                .append("                    </tr>")
                .append("                </thead>")
                .append("                <tbody>");

            // Loop and add the formatted Table Rows dynamically
            for (Vehicle v : fleet) {
                String badgeClass = v.status.equals("AVAILABLE") ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger";
                String paymentBadge = v.payment.equals("PAID") ? "bg-success text-white" : (v.payment.equals("PENDING") ? "bg-warning text-dark" : "bg-light text-dark");
                
                html.append("                    <tr>")
                    .append("                        <td><span class='fw-bold text-secondary'>").append(v.id).append("</span></td>")
                    .append("                        <td><span class='fw-semibold text-dark'>").append(v.model).append("</span></td>")
                    .append("                        <td>₹").append(v.rate).append("</td>")
                    .append("                        <td><span class='badge ").append(badgeClass).append(" px-2.5 py-1.5'>").append(v.status).append("</span></td>")
                    .append("                        <td class='text-muted small'>").append(v.rentalStart).append("</td>")
                    .append("                        <td><span class='badge rounded-pill ").append(paymentBadge).append("'>").append(v.payment).append("</span></td>")
                    .append("                        <td class='fw-bold ").append(v.dues > 0 ? "text-danger" : "text-dark").append("'>₹").append(v.dues).append("</td>")
                    .append("                        <td class='text-end'>");
                
                if (v.status.equals("AVAILABLE")) {
                    html.append("                            <a href='/?action=rent_now&id=").append(v.id).append("' class='btn btn-sm btn-primary btn-action me-1'><i class='bi bi-wallet2 me-1'></i> Rent & Pay</a>")
                        .append("                            <a href='/?action=rent_later&id=").append(v.id).append("' class='btn btn-sm btn-outline-secondary btn-action'><i class='bi bi-clock-history me-1'></i> Pay Later</a>");
                } else {
                    html.append("                            <a href='/?action=return&id=").append(v.id).append("' class='btn btn-sm btn-danger btn-action'><i class='bi bi-arrow-left-right me-1'></i> Process Return</a>");
                }
                
                html.append("                        </td>")
                    .append("                    </tr>");
            }

            html.append("                </tbody>")
                .append("            </table>")
                .append("        </div>")
                .append("    </div>")
                .append("</div>")
                
                // JavaScript integration for dismissible bootstrap alert components
                .append("<script src='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js'></script>")
                .append("</body>")
                .append("</html>");

            // Send HTML standard response back over HTTP using UTF-8 to display the Rupee symbol correctly
            byte[] responseBytes = html.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}
