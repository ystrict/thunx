package eu.xenit.contentcloud.security.pbac.pdp.opa;

import eu.xenit.contentcloud.abac.opa.client.OpaClient;
import eu.xenit.contentcloud.abac.opa.client.api.CompileApi.PartialEvaluationRequest;
import eu.xenit.contentcloud.security.pbac.pdp.PolicyDecision;
import eu.xenit.contentcloud.security.pbac.pdp.PolicyDecisionPointClient;
import eu.xenit.contentcloud.security.pbac.pdp.PolicyDecisions;
import eu.xenit.contentcloud.security.pbac.pdp.RequestContext;
import java.util.Objects;
import reactor.core.publisher.Mono;

public class OpenPolicyAgentPDPClient implements PolicyDecisionPointClient {

    private final OpaClient opaClient;

    public OpenPolicyAgentPDPClient(OpaClient opaClient) {
        Objects.requireNonNull(opaClient, "opaClient is required");
        this.opaClient = opaClient;
    }

    @Override
    public <TPrincipal> Mono<PolicyDecision> conditional(TPrincipal principal, RequestContext requestContext) {
        return Mono.fromCompletionStage(() -> opaClient.compile(new PartialEvaluationRequest(null, null, null)))
                .map(response -> {
                    // list of possible partially evaluated queries from OPA
                    // we need to convert this to a single boolean expression
                    var querySet = response.getResult().getQueries();

                    var converter = new QuerySetToPbacExpressionConverter();
                    return converter.convert(querySet);
                })
                .map(expression -> {
                    // if the expression can be resolved right now, there is no remaining predicate
                    if (expression.canBeResolved()) {
                        return expression.resolve() ? PolicyDecisions.allowed() : PolicyDecisions.denied();
                    } else {
                        // there is a remaining predicate
                        return PolicyDecisions.conditional(expression);
                    }
                });
    }



}
