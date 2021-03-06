package org.hit.internetprogramming.eoh.server.action.impl;

import lombok.extern.log4j.Log4j2;
import org.hit.internetprogramming.eoh.common.comms.HttpStatus;
import org.hit.internetprogramming.eoh.common.comms.Response;
import org.hit.internetprogramming.eoh.common.graph.IGraph;
import org.hit.internetprogramming.eoh.common.graph.MatrixGraphAdapter;
import org.hit.internetprogramming.eoh.common.mat.Index;
import org.hit.internetprogramming.eoh.server.action.Action;
import org.hit.internetprogramming.eoh.server.action.ActionContext;
import org.hit.internetprogramming.eoh.server.graph.algorithm.ConnectedComponents;
import org.hit.internetprogramming.eoh.server.graph.algorithm.DFSVisit;
import org.hit.internetprogramming.eoh.server.graph.algorithm.Submarines;
import org.hit.internetprogramming.eoh.server.impl.Graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class for receiving all the submarines in a graph.<br/>
 * The graph is represented by a matrix.
 * <p>
 * Submarine rules:
 * 1. at least 2 horizontal 1-node
 * 2. at least 2 vertical 1-node
 * 3. There can be no two nodes diagonally unless
 * there are sections 1 and 2 for both.
 * 4. The minimum distance between two submarines
 * (regardless of orientation) is one square (0-node).
 *
 * @author Eden Zadikove
 * @see FindConnectedComponents
 * @since 21-July-21
 */
@Log4j2
public class FindSubmarines implements Action {
    @Override
    public Response execute(ActionContext actionContext) {
        IGraph<Index> graph = Graphs.getInstance().getGraph(actionContext.getClientInfo());
        if (graph == null) {
            return Response.error(HttpStatus.NOT_FOUND.getCode(), "No graph was initialized. Please put graph or generate one", actionContext.getRequest().isHttp());
        }
        Submarines submarines = new Submarines();
        return Response.ok(HttpStatus.OK.getCode(), submarines.findSubmarines(graph), actionContext.getRequest().isHttp());
    }
}
