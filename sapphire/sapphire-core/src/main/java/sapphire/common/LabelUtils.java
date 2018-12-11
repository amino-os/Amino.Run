package sapphire.common;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sapphire.app.NodeAffinity;
import sapphire.app.NodeSelectorRequirement;
import sapphire.app.NodeSelectorTerm;
import sapphire.app.PreferredSchedulingTerm;

public class LabelUtils {

    private static final Logger logger = Logger.getLogger(LabelUtils.class.getName());

    private LabelUtils() {}

    public static final String DoesNotExist = "!";
    public static final String Equals = "=";
    public static final String DoubleEquals = "==";
    public static final String In = "in";
    public static final String NotEquals = "!=";
    public static final String NotIn = "notin";
    public static final String Exists = "exists";
    public static final String GreaterThan = "gt";
    public static final String LessThan = "lt";

    public static final int DNS1123SubdomainMaxLength = 253;
    public static final String dns1123LabelFmt = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";
    public static final String dns1123SubdomainFmt =
            dns1123LabelFmt + "(\\." + dns1123LabelFmt + ")*";

    public static final Pattern dns1123SubdomainRegexp =
            Pattern.compile("^" + dns1123SubdomainFmt + "$");

    public static final String qnameCharFmt = "[A-Za-z0-9]";
    public static final String qnameExtCharFmt = "[-A-Za-z0-9_.]";
    public static final String qualifiedNameFmt =
            "(" + qnameCharFmt + qnameExtCharFmt + "*)?" + qnameCharFmt;
    public static final String labelValueFmt = "(" + qualifiedNameFmt + ")?";

    public static final Pattern qualifiedNameRegexp = Pattern.compile("^" + qualifiedNameFmt + "$");

    public static final int qualifiedNameMaxLength = 63;
    public static final int LabelValueMaxLength = 63;

    public static final Pattern labelValueRegexp = Pattern.compile("^" + labelValueFmt + "$");

    public static String DEFAULT_REGION = "default";
    public static String REGION_KEY = "region";
    public static String LABEL_OPT = "--labels:";
    public static String OPT_SEPARATOR = "=";
    public static String LABEL_SEPARATOR = ",";
    /**
     * Validates the given key,operator & vales are valid or not.
     *
     * @param key
     * @param Operator
     * @param vals
     * @return <code>true</code> if the given params are valid <code>false</code> otherwise.
     */
    // NodeSelectorRequirement is the constructor for a NodeSelectorRequirement.
    // If any of these rules is violated, an error is returned:
    // (1) The operator can only be In, NotIn, Equals, DoubleEquals, NotEquals, Exists, or
    // DoesNotExist.
    // (2) If the operator is In or NotIn, the values set must be non-empty.
    // (3) If the operator is Equals, DoubleEquals, or NotEquals, the values set must contain one
    // value.
    // (4) If the operator is Exists or DoesNotExist, the value set must be empty.
    // (5) If the operator is Gt or Lt, the values set must contain only one value, which will be
    // interpreted as an integer.
    // (6) The key is invalid due to its length, or sequence
    //     of characters. See validateLabelKey for more details.
    //
    // The empty string is a valid value in the input values set.
    public static boolean validateNodeSelectRequirement(
            String key, String Operator, List<String> vals) {
        if (!validateLabelKey(key)) {
            logger.warning("validateLabelKey failed" + key);
            return false;
        }

        if (!validateOperator(Operator)) {
            logger.warning("null or empty Operator is not allowed");
            return false;
        }
        switch (Operator) {
            case In:
            case NotIn:
                if (vals.size() == 0) {
                    logger.warning("for in, notin operators, values set can't be empty");
                    return false;
                }
                break;
            case Equals:
            case DoubleEquals:
            case NotEquals:
                if (vals.size() != 1) {
                    logger.warning("exact-match compatibility requires one single value");
                    return false;
                }
                break;
            case Exists:
            case DoesNotExist:
                if (vals.size() != 0) {
                    logger.warning("values set must be empty for exists and does not exist");
                    return false;
                }
                break;
            case GreaterThan:
            case LessThan:
                if (vals.size() != 1) {
                    logger.warning("for 'Gt', 'Lt' operators, exactly one value is required");
                    return false;
                }
                for (int i = 0; i < vals.size(); i++) {
                    try {
                        Integer.parseInt(vals.get(i));
                    } catch (NumberFormatException e) {
                        logger.warning("for 'Gt', 'Lt' operators, value is not valid");
                        return false;
                    }
                }
                break;
            default:
                logger.warning("operator '%v' is not recognized" + Operator);
                return false;
        }

        for (int i = 0; i < vals.size(); i++) {
            if (!validateLabelValue(vals.get(i))) {
                logger.warning("validateLabelValue failed " + vals.get(i));
                return false;
            }
        }

        return true;
    }

    public static boolean validateOperator(String operator) {

        if (operator == null || operator.isEmpty()) {
            logger.warning("null or empty operator is not allowed");
            return false;
        }
        if ((operator.equals(In))
                || (operator.equals(NotIn))
                || (operator.equals(Exists))
                || (operator.equals(DoesNotExist))
                || (operator.equals(Equals))
                || (operator.equals(NotEquals))
                || (operator.equals(DoubleEquals))
                || (operator.equals(GreaterThan))
                || (operator.equals(LessThan))) {
            return true;
        }
        return false;
    }

    /**
     * Validates the given key is valid or not.
     *
     * @param key
     * @return <code>true</code> if the given key is valid <code>false</code> otherwise.
     */
    public static boolean validateLabelKey(String key) {
        if (key == null || key.isEmpty()) {
            logger.warning("null or empty key is not allowed");
            return false;
        }
        return IsQualifiedName(key);
    }

    /**
     * Validates the given value is valid or not.
     *
     * @param value
     * @return <code>true</code> if the given value is valid <code>false</code> otherwise.
     */
    public static boolean validateLabelValue(String value) {
        if (value == null || value.isEmpty()) {
            logger.warning("null or empty val is not allowed");
            return false;
        }

        if (value.length() > LabelValueMaxLength) {
            return false;
        }
        Matcher matcher = labelValueRegexp.matcher(value);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    public static boolean IsDNS1123Subdomain(String value) {
        if (value.length() > DNS1123SubdomainMaxLength) {
            logger.warning("length is more than the max length" + value.length());
            return false;
        }
        Matcher matcher = dns1123SubdomainRegexp.matcher(value);
        if (!matcher.matches()) {
            logger.warning("matcher.matches failed");
            return false;
        }
        return true;
    }

    public static boolean IsQualifiedName(String value) {
        String[] parts = value.split("/");
        String name = null;
        String prefix = null;
        switch (parts.length) {
            case 1:
                name = parts[0];
                break;
            case 2:
                prefix = parts[0];
                name = parts[1];
                if (prefix.length() == 0) {
                    return false;
                } else if (!IsDNS1123Subdomain(prefix)) {
                    return false;
                }
                break;
            default:
                return false;
        }

        if (name.length() == 0) {
            return false;
        } else if (name.length() > qualifiedNameMaxLength) {
            return false;
        }
        Matcher matcher = qualifiedNameRegexp.matcher(name);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    public static boolean IsFullyQualifiedName(String name) {
        if (name.length() == 0) {
            logger.warning("name length is zero");
            return false;
        }
        if (!IsDNS1123Subdomain(name)) {
            logger.warning("IsDNS1123Subdomain failed");
            return false;
        }
        if (name.split(".").length < 3) {
            return false;
        }
        return true;
    }

    public static boolean validateNodeSelectorTerm(NodeSelectorTerm term) {
        if (term == null) {
            return true;
        }
        List<NodeSelectorRequirement> MatchExpressions = term.getMatchExpressions();
        List<NodeSelectorRequirement> MatchFields = term.getMatchFields();

        if (MatchExpressions != null) {
            for (int i = 0; i < MatchExpressions.size(); i++) {
                NodeSelectorRequirement req = MatchExpressions.get(i);
                if (!validateNodeSelectRequirement(
                        req.getKey(), req.getOperator(), req.getValues())) {
                    logger.warning("validateNodeSelectRequirement failed" + req);
                    return false;
                }
            }
        }

        if (MatchFields != null) {
            for (int i = 0; i < MatchFields.size(); i++) {
                NodeSelectorRequirement req = MatchFields.get(i);
                if (!validateNodeSelectRequirement(
                        req.getKey(), req.getOperator(), req.getValues())) {
                    logger.warning("validateNodeSelectRequirement failed" + req);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validatePreferredSchedulingTerm(PreferredSchedulingTerm Prefterm) {
        if (Prefterm == null) {
            return true;
        }
        if (Prefterm.getweight() < 0) {
            logger.warning("PreferredSchedulingTerm weight is lessthan zero not allowed");
            return false;
        }
        if (!validateNodeSelectorTerm(Prefterm.getNodeSelectorTerm())) {
            return false;
        }
        return true;
    }

    public static boolean validateNodeAffinity(NodeAffinity nodeAffinity) {
        if (nodeAffinity == null) {
            return true;
        }
        List<NodeSelectorTerm> reqterms = nodeAffinity.getRequireExpressions();
        List<PreferredSchedulingTerm> prefTerms = nodeAffinity.getPreferScheduling();

        if (reqterms != null) {
            for (int i = 0; i < reqterms.size(); i++) {
                if (!validateNodeSelectorTerm(reqterms.get(i))) {
                    logger.warning("validateNodeSelectorTerm failed" + reqterms.get(i));
                    return false;
                }
            }
        }
        if (prefTerms != null) {
            for (int i = 0; i < prefTerms.size(); i++) {
                if (!validatePreferredSchedulingTerm(prefTerms.get(i))) {
                    logger.warning("validatePreferredSchedulingTerm failed" + reqterms.get(i));
                    return false;
                }
            }
        }
        return true;
    }
}
