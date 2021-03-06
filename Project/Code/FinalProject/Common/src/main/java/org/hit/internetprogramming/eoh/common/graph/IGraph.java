package org.hit.internetprogramming.eoh.common.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.hit.internetprogramming.eoh.common.mat.IMatrix;

import java.util.List;

/**
 * This interface is the primary data type for Graph hierarchy<br/>
 * It represents the basic functionality required from a traversable graph.
 * @param <T> The type of elements in the graph
 * @author Nathan Dillbary, Haim Adrian
 * @since 06-Mar-21
 * @see IMatrix
 */
// Store type info to json, so we can ease serialization / deserialization
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({ @JsonSubTypes.Type(value = MatrixGraphAdapter.class, name = "matrix") })
public interface IGraph<T> {
   /**
    * @return The root element in the graph
    */
   T getRoot();

   /**
    * Get all adjacent vertices of a specified vertex.<br/>
    * An adjacent vertex might have some value in it, or empty. (For binary data, 1=has data, 0=empty)<br/>
    * If you want to get adjacent vertices with value in them, those vertices are "reachable" vertices. See {@link #getReachableVertices(Object)}
    * @param vertex The vertex to get its adjacent vertices
    * @return All adjacent vertices of the specified vertex
    */
   List<T> getAdjacentVertices(T vertex);

   /**
    * Get all reachable vertices of a specified vertex.<br/>
    * A reachable vertex is an adjacent vertex that must have a value in it. (For binary data, 1=has data, 0=empty)<br/>
    * If you want to get all adjacent vertices, use {@link #getAdjacentVertices(Object)}<br/>
    * <b>Note:</b> If specified vertex does not have value in it, the result is an empty list. Nothing is reachable from nowhere.
    * @param vertex The vertex to get its reachable vertices
    * @return All reachable vertices from the specified vertex
    */
   List<T> getReachableVertices(T vertex);

   /**
    * Get a string representing this graph.<br/>
    * The string will be formatted as a matrix, where all 0's are replaced with space, and you see 1's only.<br/>
    * For example:
    * <pre>
    * [  1  ]
    * [1 1 1]
    * [    1]
    * </pre>
    * @return A pretty presentation of this graph
    * @see IMatrix#printMatrix()
    * @see #toString()
    */
   String printGraph();

   /**
    * Get a string representing this graph.<br/>
    * The string will be formatted as a matrix, where all 0's are replaced with space, and you see 1's only.<br/>
    * For example:
    * <pre>
    * [[ , 1,  ],
    * [1, 1, 1],
    * [ ,  , 1]]
    * </pre>
    * @return A pretty presentation of this graph
    * @see IMatrix#toString()
    * @see #printGraph()
    */
   String toString();

   /**
    * This method created in order to let the user know how many references this graph holds in memory.<br/>
    * When the implementation is of a matrix, the result is ROWS*COLS. Otherwise, the result is V*E, where
    * V is the amount of vertices, and E is the amount of edges.
    * @return Size of this graph
    */
   int getGraphSize();

   /**
    * Get all vertices in this graph.<br/>
    * This method can take {@link #getGraphSize()} operations in order to collect all vertices in the graph.
    * @return All of the vertices in this graph
    */
   List<T> getVertices();

   /**
    * Get all edges in this graph.<br/>
    * This method can take {@link #getGraphSize()}*4 operations in order to collect all edges in the graph.
    * @return All of the edges in this graph
    */
   List<Pair<T, T>> getEdges();

   /**
    * Get the value of a vertex in this graph
    * @param vertex The vertex to get its value
    * @param <V> Type of the value of vertices in this graph
    * @return Value of the specified vertex
    */
   <V> V getValue(T vertex);

   /**
    * Tests if a given vertex belongs to this graph. We need to test if vertices are part of a graph when
    * looking for submarines.
    * @param vertex The vertex to check if it belongs to this graph
    * @return Whether vertex belongs to this graph or not
    */
   boolean contains(T vertex);
}
