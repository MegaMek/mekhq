# Long-Term Effects of Injuries on Mortality Rates

## Technical Specification

## 1. Base Concept

After recovery from injuries, certain types of trauma leave lasting effects that increase mortality rates. These are
separate from permanent injuries (like lost limbs) and represent the overall weakening of health due to past trauma.

## 2. Long-Term Impact Categories

### 2.1 Minor Impact (+2% to base mortality)

Previous injuries that healed well:

- Simple broken bones
- Minor burns
- Concussions (single incident)
- Deep cuts/lacerations
- Temporary organ bruising

### 2.2 Moderate Impact (+5% to base mortality)

More serious previous trauma:

- Multiple broken bones from same incident
- Major burns (recovered)
- Multiple concussions
- Internal injuries (healed)
- Combat fatigue/sustained stress
- Multiple hospital stays
- Exposure to extreme environments

### 2.3 Significant Impact (+10% to base mortality)

Serious previous trauma:

- Multiple serious injuries in career
- Major internal organ damage (healed)
- Severe burns (recovered)
- Brain trauma (recovered)
- Extended coma recovery
- Life-support requirement history
- Multiple surgeries from same incident

### 2.4 Critical Impact (+15% to base mortality)

Most severe previous trauma:

- Multiple near-death experiences
- History of complete system failure
- Multiple major organ repairs
- Extensive reconstructive surgery
- Extended life support periods
- Severe brain trauma recovery

## 3. Stacking Rules

### 3.1 Basic Stacking

- First recorded trauma: Full value
- Second recorded trauma: 75% of value
- Third recorded trauma: 50% of value
- Additional trauma: 25% of value
- Maximum total: +25% to base mortality rate

Example:

```
History:
- Major internal injuries (Significant: +10%)
- Multiple broken bones (Moderate: +3.75% [75% of 5%])
- Multiple concussions (Moderate: +2.5% [50% of 5%])
Total: +16.25% increase to base mortality rate
```

### 3.2 Recovery Factor

Time since trauma affects impact:

- Recent (< 1 year): 100% of modifier
- Medium (1-5 years): 75% of modifier
- Long-term (5-10 years): 50% of modifier
- Very long-term (>10 years): 25% of modifier

## 4. Medical Technology Impact

### 4.1 Faction Modifiers

Advanced medical care reduces long-term impacts:

- Clan: Reduce effects by 30%
- Major Inner Sphere: Reduce by 20%
- Minor Inner Sphere: Reduce by 10%
- Periphery: No reduction

### 4.2 Era Modifiers

Medical technology advancement:

- Star League: -30% to long-term effects
- Succession Wars: No modifier
- Clan Invasion Era: -10% to long-term effects
- Republic Era: -15% to long-term effects
- Dark Age: -20% to long-term effects

## 5. Implementation Structure

### 5.1 Tracking System

```java
public class InjuryHistory {
    private List<PastTrauma> traumaHistory;
    private MedicalTechLevel medicalLevel;
    private Era era;

    public double calculateLongTermEffect() {
        double totalModifier = 0.0;
        double stackingFactor = 1.0;

        for (PastTrauma trauma : sortByImpact(traumaHistory)) {
            double baseEffect = trauma.getImpact().getModifier();
            double timeEffect = trauma.getTimeFactor();

            totalModifier += (baseEffect * stackingFactor * timeEffect);
            stackingFactor *= 0.75; // Reduce impact of each additional trauma
        }

        // Apply medical technology reduction
        totalModifier *= (1.0 - medicalLevel.getReduction());
        totalModifier *= (1.0 - era.getMedicalModifier());

        return Math.min(totalModifier, 0.25); // Cap at 25%
    }
}
```

## 6. Example Cases

### 6.1 Veteran MechWarrior

```
History:
- Cockpit trauma 10 years ago (Significant: +10% × 0.25 for time = +2.5%)
- Multiple bone breaks 2 years ago (Moderate: +5% × 0.75 × 0.75 for stacking = +2.81%)
- Recent internal injuries (Minor: +2% × 0.5 for stacking = +1%)

Location: Federated Suns (-20% to effects)
Era: Clan Invasion (-10% to effects)

Final Modifier: +4.52% to base mortality rate
```

### 6.2 Infantry Veteran

```
History:
- Multiple combat injuries 5 years ago (Significant: +10% × 0.5 for time = +5%)
- Severe burns 3 years ago (Moderate: +5% × 0.75 × 0.75 for stacking = +2.81%)
- Recent head trauma (Moderate: +5% × 0.5 for stacking = +2.5%)

Location: Periphery (No reduction)
Era: Succession Wars (No modifier)

Final Modifier: +10.31% to base mortality rate
```

## 7. Integration with Random Death System

### 7.1 Modifier Application

The long-term injury modifier is applied to the base mortality rate before other current condition modifiers:

```java
public double calculateFinalMortalityRate(Person person) {
    double baseRate = getBaseRate(person.getAge(), person.getGender());
    double longTermMod = person.getInjuryHistory().calculateLongTermEffect();

    // Apply long-term injury modifier first
    baseRate *= (1 + longTermMod);

    // Then apply other modifiers (faction, current conditions, etc.)
    return applyCurrentModifiers(baseRate, person);
}
```

Would you like me to expand on any aspect of these long-term effects or provide additional implementation details?
