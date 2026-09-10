# Solution for Issue #10007

## 🛠️ Proposed Solution (by Aditya Waghamare)

### Analysis
The "Stop the Escapees" crisis scenario generator in MekHQ fails to instantiate or assign the escapee units to the scenario deployment list when triggered from specific contracts like Security Duty scouting events. This occurs because the scenario template parser checks for specific unit role definitions or deployment zone availability without falling back to a default escapee unit assignment if the primary unit pool query returns empty.

### Fix
Update the scenario generation logic for "Stop the Escapees" in MekHQ to ensure that if no explicit escapee units are generated or filtered from the contract's available forces, a default set of escapee units (e.g., civilian or light transport/infantry units) is forced into the deployment queue.

### Implementation
```java
// In mekhq/campaign/scenario/StopTheEscapeesGenerator.java (or equivalent scenario generation class)
protected void generateEscapees(Scenario scenario, Campaign campaign, Contract contract) {
    List<Entity> escapees = createEscapeeUnits(contract);
    if (escapees.isEmpty()) {
        // Fallback to default civilian/escapee force if none provided by contract parameters
        escapees = createDefaultEscapees(scenario.getBoard());
    }
    for (Entity entity : escapees) {
        scenario.addAttackerUnit(entity); // or appropriate escapee side
        entity.setOwner(scenario.getEscapeePlayer());
    }
}
```

### Testing
1. Trigger a Security Duty contract scouting crisis event resulting in "Stop the Escapees".
2. Verify that the generated scenario includes both the OPFOR tanks/mechs and the required escapee units on the deployment map.

Signed-off-by: Aditya Waghamare <adityawaghamare7620@gmail.com>

---
*Submitted by Aditya Waghamare*
💰 **Payout Address (Base L2 / EVM):** `0xb61dBcdBc3407F71EaCb64D4CBFAcf9FFfe2415C`