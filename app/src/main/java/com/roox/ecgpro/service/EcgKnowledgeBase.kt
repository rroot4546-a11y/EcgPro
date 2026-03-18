package com.roox.ecgpro.service

/**
 * Built-in ECG diagnostic criteria database.
 * Based on: AHA/ACC/HRS 2024 Guidelines, Braunwald's Heart Disease 12th Ed,
 * Harrison's 21st Ed, Marriott's Practical ECG 13th Ed, Chou's ECG 8th Ed.
 */
object EcgKnowledgeBase {

    data class DiagCriteria(
        val code: String,
        val name: String,
        val group: String,   // Rhythm, Conduction, Morphology, Ischemia
        val criteria: String,
        val keyLeads: String,
        val urgency: String  // routine, urgent, emergent
    )

    val diagnoses = listOf(
        // === RHYTHM ===
        DiagCriteria("sinrhy", "Normal Sinus Rhythm", "Rhythm",
            "Rate 60-100 bpm, regular rhythm, upright P in I/II/aVF, inverted in aVR, 1:1 P:QRS, constant PR 120-200ms, narrow QRS <120ms",
            "II, V1", "routine"),
        DiagCriteria("sinbrad", "Sinus Bradycardia", "Rhythm",
            "Rate <60 bpm, normal P waves, regular rhythm, normal PR and QRS. Causes: athletes, sleep, beta-blockers, hypothyroidism, sick sinus",
            "II", "routine"),
        DiagCriteria("sintach", "Sinus Tachycardia", "Rhythm",
            "Rate >100 bpm (usually 100-150), normal P morphology, gradual onset/offset. Causes: fever, pain, anxiety, hypovolemia, PE, thyrotoxicosis",
            "II, V1", "routine"),
        DiagCriteria("afib", "Atrial Fibrillation", "Rhythm",
            "Irregularly irregular RR intervals, absent discrete P waves, fibrillatory baseline (f waves), variable ventricular rate. CHA2DS2-VASc for stroke risk",
            "II, V1", "urgent"),
        DiagCriteria("afibrapid", "AFib with Rapid Ventricular Response", "Rhythm",
            "AFib criteria + ventricular rate >100 bpm. Risk of hemodynamic compromise. Consider rate control (diltiazem, metoprolol) or cardioversion if unstable",
            "II, V1", "urgent"),
        DiagCriteria("afibslow", "AFib with Slow Ventricular Response", "Rhythm",
            "AFib criteria + ventricular rate <60 bpm. Suggests AV nodal disease, digoxin toxicity, or excessive rate control",
            "II, V1", "urgent"),
        DiagCriteria("aflut", "Atrial Flutter", "Rhythm",
            "Sawtooth flutter waves (F waves) at ~300/min, typically 2:1 block giving rate ~150. Best seen in II, III, aVF, V1. Regular or regularly irregular",
            "II, III, aVF, V1", "urgent"),
        DiagCriteria("svt", "Supraventricular Tachycardia", "Rhythm",
            "Rate 150-250 bpm, narrow QRS, regular, abrupt onset/offset. Includes AVNRT (most common), AVRT, atrial tachycardia. Vagal maneuvers or adenosine",
            "II, V1", "urgent"),
        DiagCriteria("junrhy", "Junctional Rhythm", "Rhythm",
            "Rate 40-60 bpm, narrow QRS, absent/retrograde P waves (inverted in II/III/aVF). P may precede, follow, or be hidden in QRS",
            "II, III, aVF", "urgent"),
        DiagCriteria("accjunrhy", "Accelerated Junctional Rhythm", "Rhythm",
            "Rate 60-100 bpm, junctional focus. Same P wave criteria as junctional rhythm but faster. Consider digoxin toxicity, post-cardiac surgery",
            "II, V1", "urgent"),
        DiagCriteria("wqrstach", "Wide Complex Tachycardia", "Rhythm",
            "Rate >100 bpm, QRS >120ms. DDx: VT (most common and most dangerous), SVT with aberrancy, pre-excited tachycardia. When in doubt, treat as VT",
            "V1, V6, aVR", "emergent"),
        DiagCriteria("idiovenrhy", "Idioventricular Rhythm", "Rhythm",
            "Rate 20-40 bpm, wide QRS >120ms, no P waves or AV dissociation. Escape rhythm. Accelerated (AIVR) 40-100 bpm seen in reperfusion",
            "V1-V6", "emergent"),
        DiagCriteria("pcom", "Premature Complexes", "Rhythm",
            "PACs: premature narrow QRS with abnormal P wave. PVCs: premature wide QRS >120ms, no preceding P, compensatory pause. Frequent PVCs >10% may cause cardiomyopathy",
            "II, V1", "routine"),
        DiagCriteria("pacerhy", "Pacemaker Rhythm", "Rhythm",
            "Pacing spikes before P waves (atrial pacing) and/or QRS (ventricular pacing). Wide QRS with LBBB pattern if RV paced. Check capture and sensing",
            "II, V1-V6", "routine"),

        // === CONDUCTION ===
        DiagCriteria("avblock1", "First Degree AV Block", "Conduction",
            "PR interval >200ms, every P followed by QRS. Benign, no treatment usually. Causes: AV nodal delay, vagal tone, drugs (beta-blockers, CCBs, digoxin)",
            "II, V1", "routine"),
        DiagCriteria("avblock2w", "Second Degree AV Block Type I (Wenckebach)", "Conduction",
            "Progressive PR prolongation until dropped QRS. Grouped beating. Usually at AV node level. Often benign (athletes, sleep). Mobitz I",
            "II", "routine"),
        DiagCriteria("avblockhd", "High Degree AV Block (Mobitz II / Complete)", "Conduction",
            "Mobitz II: constant PR then sudden dropped QRS without prolongation. Infra-nodal. 3rd degree: complete AV dissociation, atrial rate > ventricular rate. EMERGENCY - may need pacing",
            "II, V1", "emergent"),
        DiagCriteria("rbbb", "Right Bundle Branch Block", "Conduction",
            "QRS ≥120ms, rsR' or rSR' in V1-V2 (M-shaped), wide slurred S in I and V6. Normal axis. ST-T changes secondary. Causes: PE, RV strain, ASD, IHD",
            "V1, V2, I, V6", "routine"),
        DiagCriteria("irbbb", "Incomplete RBBB", "Conduction",
            "QRS 100-120ms with rSr' in V1-V2. Same morphology as RBBB but narrower QRS. Often normal variant",
            "V1, V2", "routine"),
        DiagCriteria("lbbb", "Left Bundle Branch Block", "Conduction",
            "QRS ≥120ms, broad notched R in I, aVL, V5-V6, deep S in V1-V3, absent septal q in I/V5-V6. ST-T discordant to QRS. NEW LBBB + chest pain = STEMI equivalent",
            "V1, V5, V6, I, aVL", "urgent"),
        DiagCriteria("ilbbb", "Incomplete LBBB", "Conduction",
            "QRS 100-120ms with LBBB morphology. Notched R in lateral leads but narrower",
            "V5, V6, I", "routine"),
        DiagCriteria("lafb", "Left Anterior Fascicular Block", "Conduction",
            "Left axis deviation (-45° to -90°), small q in I/aVL, small r in II/III/aVF, QRS <120ms. Most common fascicular block",
            "I, aVL, II, III, aVF", "routine"),
        DiagCriteria("lpfb", "Left Posterior Fascicular Block", "Conduction",
            "Right axis deviation (>+120°), small r in I/aVL, small q in II/III/aVF, QRS <120ms. Must exclude RVH and other causes of RAD",
            "I, aVL, II, III, aVF", "routine"),
        DiagCriteria("bifasblocka", "Bifascicular Block (RBBB + LAFB)", "Conduction",
            "RBBB morphology + left axis deviation. Risk of progression to complete heart block. Most common bifascicular block",
            "V1, I, aVL", "urgent"),
        DiagCriteria("bifasblockp", "Bifascicular Block (RBBB + LPFB)", "Conduction",
            "RBBB morphology + right axis deviation (>+120°). Less common. Exclude RVH first",
            "V1, II, III, aVF", "urgent"),
        DiagCriteria("trifasblocka", "Trifascicular Block", "Conduction",
            "Bifascicular block + first degree AV block (PR >200ms). High risk of complete heart block. Consider pacemaker evaluation",
            "V1, II", "urgent"),
        DiagCriteria("ivcondelay", "Nonspecific Intraventricular Conduction Delay", "Conduction",
            "QRS ≥120ms but does not meet criteria for RBBB or LBBB. Diffuse conduction disease",
            "V1-V6", "routine"),

        // === MORPHOLOGY ===
        DiagCriteria("venhyp", "Ventricular Hypertrophy", "Morphology",
            "LVH: Sokolow-Lyon (SV1+RV5/V6 ≥35mm), Cornell (RaVL+SV3 >28mm men, >20mm women). Strain pattern: ST depression + T inversion in lateral leads. RVH: R>S in V1, RAD, RV strain pattern, P pulmonale",
            "V1, V5, V6, aVL, I", "routine"),
        DiagCriteria("atrenl", "Atrial Enlargement", "Morphology",
            "LAE: P wave >120ms in II, biphasic P in V1 with terminal negative >1mm deep and >40ms. P mitrale. RAE: P wave >2.5mm tall in II (P pulmonale), initial positive >1.5mm in V1",
            "II, V1", "routine"),
        DiagCriteria("longqtsyn", "Long QT Syndrome", "Morphology",
            "QTc >470ms (men) or >480ms (women). Bazett: QTc = QT/√RR. Risk of Torsades de Pointes. Causes: congenital (Romano-Ward, Jervell-Lange-Nielsen), drugs (sotalol, amiodarone, erythromycin, haloperidol), electrolytes (hypoK, hypoMg, hypoCa)",
            "II, V5", "urgent"),
        DiagCriteria("shortqtsyn", "Short QT Syndrome", "Morphology",
            "QTc <340ms. Tall peaked symmetric T waves. Risk of AF and VF. Rare genetic channelopathy",
            "II, V5", "urgent"),

        // === ISCHEMIA ===
        DiagCriteria("stemia", "STEMI (ST-Elevation MI)", "Ischemia",
            "ST elevation ≥1mm in 2+ contiguous leads (≥2mm in V1-V3 men, ≥1.5mm V1-V3 women). Reciprocal ST depression. Territories: Anterior (V1-V4, LAD), Inferior (II,III,aVF, RCA), Lateral (I,aVL,V5-V6, LCx), Posterior (reciprocal V1-V3 depression, tall R V1-V2). New LBBB = STEMI equivalent. EMERGENT cath lab activation",
            "ALL LEADS", "emergent"),
        DiagCriteria("nstemi", "NSTEMI / Acute Coronary Syndrome", "Ischemia",
            "ST depression ≥0.5mm in 2+ contiguous leads, T wave inversion ≥1mm. Dynamic changes. Wellens syndrome (deep symmetric T inversion V1-V4 = critical LAD stenosis). de Winter pattern (upsloping ST depression V1-V6 = proximal LAD). Troponin elevation confirms NSTEMI",
            "V1-V6, II, III, aVF", "emergent")
    )

    /** Get criteria for a specific diagnosis code */
    fun getCriteria(code: String): DiagCriteria? = diagnoses.find { it.code == code }

    /** Get all diagnoses in a group */
    fun byGroup(group: String): List<DiagCriteria> = diagnoses.filter { it.group == group }

    /** Build reference text for AI prompt */
    fun buildPromptReference(): String = buildString {
        append("BUILT-IN ECG DIAGNOSTIC CRITERIA DATABASE:\n\n")
        for (group in listOf("Rhythm", "Conduction", "Morphology", "Ischemia")) {
            append("=== ${group.uppercase()} ===\n")
            for (d in byGroup(group)) {
                append("${d.code} | ${d.name} | Key leads: ${d.keyLeads} | ${d.urgency.uppercase()}\n")
                append("  Criteria: ${d.criteria}\n\n")
            }
        }
    }

    /** Short summary for result display */
    fun getDiagnosisSummary(code: String): String {
        val d = getCriteria(code) ?: return code
        return "${d.name} — ${d.criteria.take(100)}..."
    }
}
