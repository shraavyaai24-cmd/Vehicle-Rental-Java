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
        String rentedByUserId;
        int rentedDays; 

        public Vehicle(String id, String model, double rate) {
            this.id = id;
            this.model = model;
            this.rate = rate;
            this.status = "AVAILABLE";
            this.rentedByUserId = "NONE";
            this.rentedDays = 0;
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
        System.out.println("Rental Application operating on port: " + port);
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
            
            int daysInput = 1;
            try {
                if (params.containsKey("days")) {
                    daysInput = Integer.parseInt(params.get("days"));
                }
            } catch (NumberFormatException e) {
                daysInput = 1;
            }

            String alertMessage = "";

            User currentUser = users.stream()
                    .filter(u -> u.id.equals(CURRENT_LOGGED_IN_USER_ID))
                    .findFirst()
                    .orElse(null);

            if (action != null && vehicleId != null && currentUser != null) {
                for (Vehicle v : fleet) {
                    if (v.id.equals(vehicleId)) {
                        
                        if (action.equals("rent_now") || action.equals("rent_later")) {
                            if (currentUser.dues > 0) {
                                alertMessage = "<div class='alert alert-danger d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                        + "    <i class='bi bi-exclamation-octagon-fill me-2 fs-5'></i>"
                                        + "    <div><strong>Transaction Blocked!</strong> Clear your total outstanding deposit of ₹" + currentUser.dues + " before making another booking.</div>"
                                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                        + "</div>";
                            } 
                            else if (daysInput < 1 || daysInput > 10) {
                                alertMessage = "<div class='alert alert-danger d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                        + "    <i class='bi bi-shield-slash-fill me-2 fs-5'></i>"
                                        + "    <div><strong>Invalid Input Policy!</strong> Rental timeline allocations are constrained between 1 to 10 days only.</div>"
                                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                        + "</div>";
                            } 
                            else {
                                v.status = "RENTED";
                                v.rentedByUserId = currentUser.id;
                                v.rentedDays = daysInput; 
                                
                                double totalCost = v.rate * v.rentedDays;
                                
                                if (action.equals("rent_now")) {
                                    currentUser.dues = 0.0;
                                    alertMessage = "<div class='alert alert-success d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                            + "    <i class='bi bi-check-circle-fill me-2 fs-5'></i>"
                                            + "    <div><strong>Booking Confirmed!</strong> Rented " + v.model + " for <strong>" + v.rentedDays + " days</strong>.<br>"
                                            + "    <span class='badge bg-success mt-1 text-white fs-6'>Actual Payment Made Upfront: ₹" + totalCost + "</span> (Total Rent: ₹" + totalCost + ")</div>"
                                            + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                            + "</div>";
                                } else {
                                    double depositAmount = totalCost * 0.10;
                                    currentUser.dues = depositAmount; 
                                    alertMessage = "<div class='alert alert-warning d-flex align-items-center alert-dismissible fade show' role='alert'>"
                                            + "    <i class='bi bi-exclamation-triangle-fill me-2 fs-5'></i>"
                                            + "    <div><strong>Booking Switched to Deferred Scheme!</strong> " + v.model + " reserved for <strong>" + v.rentedDays + " days</strong>.<br>"
                                            + "    <span class='badge bg-warning mt-1 text-dark fs-6'>Actual Upfront Deposit Charged: ₹" + depositAmount + "</span> (Total Expected Rent: ₹" + totalCost + ")</div>"
                                            + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                            + "</div>";
                                }
                            }
                        } 
                        else if (action.equals("return")) {
                            if (v.rentedByUserId.equals(currentUser.id)) {
                                v.status = "AVAILABLE";
                                v.rentedByUserId = "NONE";
                                v.rentedDays = 0;
                                currentUser.dues = 0.0;
                                
                                alertMessage = "<div class='alert alert-info d-flex flex-column alert-dismissible fade show' role='alert'>"
                                        + "    <div class='d-flex align-items-center'>"
                                        + "        <i class='bi bi-arrow-left-right me-2 fs-5'></i>"
                                        + "        <div><strong>Vehicle Returned:</strong> " + v.model + " successfully brought back to deployment grid.</div>"
                                        + "    </div>"
                                        + "    <div class='mt-2 ps-4 text-success fw-bold'><i class='bi bi-shield-check me-1'></i> Refund Notification: The 10% initial holding deposit has been fully returned and released to the operator file dashboard.</div>"
                                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                                        + "</div>";
                            }
                        }
                        break;
                    }
                }
            }

            if ("clear_dues".equals(action) && currentUser != null) {
                currentUser.dues = 0.0;
                alertMessage = "<div class='alert alert-success d-flex align-items-center alert-dismissible fade show' role='alert'>"
                        + "    <i class='bi bi-shield-check me-2 fs-5'></i>"
                        + "    <div><strong>Account Settled!</strong> Outstanding balances resolved. Access blocks deactivated.</div>"
                        + "    <button type='button' class='btn-close' data-bs-dismiss='alert'></button>"
                        + "</div>";
            }

            // Calculate active total balance for display block
            double activeTotalBalance = 0.0;
            for (Vehicle v : fleet) {
                if (v.rentedByUserId.equals(currentUser.id)) {
                    activeTotalBalance = v.rate * v.rentedDays;
                    break;
                }
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<title>Enterprise Fleet Operator</title>")
                .append("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>")
                .append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css'>")
                .append("<style>body { background-color: #f4f6f9; font-family: 'Segoe UI', system-ui, sans-serif; } .card-shadow { box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.05); border: none; border-radius: 12px; }</style>")
                .append("<script>")
                .append("function processRental(action, vehicleId) {")
                .append("    var selectEl = document.getElementById('days-' + vehicleId);")
                .append("    var days = selectEl ? selectEl.value : 1;")
                .append("    window.location.href = '/?action=' + action + '&id=' + vehicleId + '&days=' + days;")
                .append("}")
                .append("</script>")
                .append("</head><body>");
                
                html.append("<nav class='navbar navbar-expand-lg navbar-dark bg-dark py-3 mb-4 shadow-sm'>")
                .append("    <div class='container'>")
                .append("        <a class='navbar-brand fw-bold text-white' href='/'><i class='bi bi-car-front-fill me-2 text-warning'></i>VEHICLE MANAGEMENT APP</a>")
                .append("        <span class='navbar-text text-white'><i class='bi bi-shield-lock-fill text-warning me-1'></i> Portal Operator: <strong class='text-info'>").append(currentUser.name).append("</strong></span>")
                .append("    </div>")
                .append("</nav>");
                
                html.append("<div class='container'>").append(alertMessage)

                // Financial Control Board - Three-Column Widget Layout
                .append("    <div class='row g-3 mb-4'>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 h-100 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted small mb-1'>Rental Clearance Checks</h6>")
                .append(currentUser.dues > 0 ? "<h4 class='fw-bold text-danger mb-0'><i class='bi bi-lock-fill me-1'></i> PROFILE SUSPENDED</h4>" : "<h4 class='fw-bold text-success mb-0'><i class='bi bi-unlock-fill me-1'></i> SYSTEM CLEAR</h4>").append("</div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 h-100 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted small mb-1'>Total Deposit</h6><h3 class='fw-bold mb-0 text-danger'>₹").append(currentUser.dues).append("</h3></div>")
                .append("                <div>").append(currentUser.dues > 0 ? "<a href='/?action=clear_dues' class='btn btn-success btn-sm fw-bold'><i class='bi bi-currency-rupee me-1'></i> Clear</a>" : "").append("</div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-4'>")
                .append("            <div class='card card-shadow bg-white p-3 h-100 d-flex flex-row align-items-center justify-content-between'>")
                .append("                <div><h6 class='text-muted small mb-1'>Total Balance</h6><h3 class='fw-bold mb-0 text-dark'>₹").append(activeTotalBalance).append("</h3></div>")
                .append("                <div class='text-secondary'><i class='bi bi-cash-stack fs-3'></i></div>")
                .append("            </div>")
                .append("        </div>")
                .append("    </div>")

                // Double Notice Segment: System Policies & Front Desk Operational Security Guidelines
                .append("    <div class='row g-3 mb-4'>")
                .append("        <div class='col-md-6'>")
                .append("            <div class='card card-shadow bg-light border-start border-warning border-3 p-3 h-100'>")
                .append("                <div class='d-flex'>")
                .append("                    <div class='text-warning me-3'><i class='bi bi-journal-text fs-3'></i></div>")
                .append("                    <div>")
                .append("                        <h6 class='fw-bold text-dark mb-1'>SYSTEM POLICY NOTE FOR OPERATORS</h6>")
                .append("                        <p class='text-muted small mb-0'>")
                .append("                            Attention Operator: Selecting <strong>'Rent Now'</strong> registers immediate full-sum settlement. Selecting <strong>'Pay Later'</strong> triggers a deferred billing model where a deposit is 10% of rent and is charged directly to account liabilities upfront.")
                .append("                        </p>")
                .append("                    </div>")
                .append("                </div>")
                .append("            </div>")
                .append("        </div>")
                .append("        <div class='col-md-6'>")
                .append("            <div class='card card-shadow bg-light border-start border-danger border-3 p-3 h-100'>")
                .append("                <div class='d-flex'>")
                .append("                    <div class='text-danger me-3'><i class='bi bi-shield-exclamation fs-3'></i></div>")
                .append("                    <div>")
                .append("                        <h6 class='fw-bold text-dark mb-1'>NOTE TO FRONT DESK</h6>")
                .append("                        <p class='text-muted small mb-0'>")
                .append("                            If pay later is decided, keep a valid physical copy of an official document such as DL, Aadhar, PAN card etc. If the vehicle is not returned after the stipulated time, report said vehicle as <strong>stolen</strong>. If full rent is not paid, report it as <strong>fraud</strong>.")
                .append("                        </p>")
                .append("                    </div>")
                .append("                </div>")
                .append("            </div>")
                .append("        </div>")
                .append("    </div>")

                .append("    <div class='card card-shadow p-4 bg-white mb-5'>")
                .append("        <h4 class='fw-bold mb-4 text-dark text-uppercase fs-5 letter-spacing'>Core Fleet Allocation Registry</h4>")
                .append("        <div class='table-responsive'>")
                .append("            <table class='table table-hover align-middle mb-0'>")
                .append("                <thead class='table-light text-secondary small'>")
                .append("                    <tr><th>ID</th><th>Vehicle Specifications</th><th>Base Tariff Rate</th><th>Status</th><th>Rental Duration</th><th>Current Renter</th><th class='text-end'>Available Directives</th></tr>")
                .append("                </thead><tbody>");

            for (Vehicle v : fleet) {
                String badgeClass = v.status.equals("AVAILABLE") ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger";
                String renterText = v.rentedByUserId.equals(currentUser.id) ? "You" : (v.rentedByUserId.equals("NONE") ? "N/A" : v.rentedByUserId);
                String durationText = v.status.equals("RENTED") ? v.rentedDays + " Days" : "—";
                
                html.append("                    <tr>")
                    .append("                        <td><span class='fw-bold text-secondary'>").append(v.id).append("</span></td>")
                    .append("                        <td><span class='fw-semibold text-dark'>").append(v.model).append("</span></td>")
                    .append("                        <td>₹").append(v.rate).append("/day</td>")
                    .append("                        <td><span class='badge ").append(badgeClass).append(" px-2.5 py-1.5'>").append(v.status).append("</span></td>")
                    .append("                        <td class='fw-semibold text-dark'>").append(durationText).append("</td>")
                    .append("                        <td class='text-muted small'>").append(renterText).append("</td>")
                    .append("                        <td class='text-end'>");
                
                if (v.status.equals("AVAILABLE")) {
                    if (currentUser.dues > 0) {
                        html.append("                            <button class='btn btn-sm btn-secondary opacity-50 me-1' disabled><i class='bi bi-lock-fill me-1'></i> Locked</button>");
                    } else {
                        html.append("                            <div class='d-inline-block me-2 align-middle'>")
                            .append("                                <select id='days-").append(v.id).append("' class='form-select form-select-sm' style='width: 95px;'>");
                        
                        for (int i = 1; i <= 10; i++) {
                            html.append("                                    <option value='").append(i).append("'>").append(i).append(i == 1 ? " Day" : " Days").append("</option>");
                        }
                        
                        html.append("                                </select>")
                            .append("                            </div>")
                            .append("                            <button onclick=\"processRental('rent_now', '").append(v.id).append("')\" class='btn btn-sm btn-primary me-1'><i class='bi bi-wallet2 me-1'></i> Rent Now</button>")
                            .append("                            <button onclick=\"processRental('rent_later', '").append(v.id).append("')\" class='btn btn-sm btn-outline-secondary'><i class='bi bi-clock-history me-1'></i> Pay Later</button>");
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
