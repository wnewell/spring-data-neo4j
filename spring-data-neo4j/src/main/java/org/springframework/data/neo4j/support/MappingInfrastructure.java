/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.mapping.*;
import org.springframework.data.neo4j.support.node.EntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategies;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.transaction.PlatformTransactionManager;

import javax.validation.Validator;

/**
 * @author mh
 * @since 17.10.11
 */
public class MappingInfrastructure {
    private ConversionService conversionService;
    private Validator validator;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;

    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;

    private TypeRepresentationStrategyFactory typeRepresentationStrategyFactory;

    private Neo4jMappingContext mappingContext;
    private CypherQueryExecutor cypherQueryExecutor;
    private EntityStateHandler entityStateHandler;
    private Neo4jEntityPersister entityPersister;
    private EntityStateFactory<Node> nodeEntityStateFactory;
    private EntityStateFactory<Relationship> relationshipEntityStateFactory;
    private EntityRemover entityRemover;
    private TypeRepresentationStrategies typeRepresentationStrategies;
    private EntityInstantiator<Relationship> relationshipEntityInstantiator;
    private EntityInstantiator<Node> nodeEntityInstantiator;
    private PlatformTransactionManager transactionManager;
    private ResultConverter resultConverter;
    private IndexProvider indexProvider;
    private GraphDatabaseService graphDatabaseService;
    private GraphDatabase graphDatabase;

    public MappingInfrastructure(GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        this.graphDatabase = graphDatabase;
        this.transactionManager = transactionManager;
    }

    public MappingInfrastructure() {
    }

    // @PostConstruct // TODO
    public void postConstruct() {
        if (this.mappingContext == null) this.mappingContext = new Neo4jMappingContext();
        if (this.graphDatabase == null) {
            this.graphDatabase = new DelegatingGraphDatabase(graphDatabaseService);
        }
        if (nodeEntityInstantiator == null) {
            nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        }
        if (relationshipEntityInstantiator == null) {
            relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        }
        if (this.typeRepresentationStrategyFactory==null) {
            this.typeRepresentationStrategyFactory = new TypeRepresentationStrategyFactory(graphDatabase);
        }
        if (this.nodeTypeRepresentationStrategy == null) {
            this.nodeTypeRepresentationStrategy = typeRepresentationStrategyFactory.getNodeTypeRepresentationStrategy();
        }
        if (this.relationshipTypeRepresentationStrategy == null) {
            this.relationshipTypeRepresentationStrategy = typeRepresentationStrategyFactory.getRelationshipTypeRepresentationStrategy();
        }
        this.typeRepresentationStrategies = new TypeRepresentationStrategies(mappingContext, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy);

        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, graphDatabase);
        EntityTools<Node> nodeEntityTools = new EntityTools<Node>(nodeTypeRepresentationStrategy, nodeEntityStateFactory, nodeEntityInstantiator);
        EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(relationshipTypeRepresentationStrategy, relationshipEntityStateFactory, relationshipEntityInstantiator);
        this.entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools, mappingContext, entityStateHandler);
        this.entityRemover = new EntityRemover(this.entityStateHandler, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy, graphDatabase);
        this.indexProvider = new IndexProvider(mappingContext, graphDatabase);
        if (this.resultConverter==null) {
            this.resultConverter = new EntityResultConverter<Object, Object>(conversionService,entityPersister);
        }
        this.graphDatabase.setResultConverter(resultConverter);
        this.cypherQueryExecutor = new CypherQueryExecutor(graphDatabase.queryEngineFor(QueryType.Cypher, resultConverter));
    }


    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setRelationshipEntityInstantiator(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    public void setNodeEntityInstantiator(EntityInstantiator<Node> nodeEntityInstantiator) {
        this.nodeEntityInstantiator = nodeEntityInstantiator;
    }


    public void setEntityStateHandler(EntityStateHandler entityStateHandler) {
        this.entityStateHandler = entityStateHandler;
    }

    public void setNodeEntityStateFactory(EntityStateFactory<Node> nodeEntityStateFactory) {
        this.nodeEntityStateFactory = nodeEntityStateFactory;
    }

    public void setRelationshipEntityStateFactory(EntityStateFactory<Relationship> relationshipEntityStateFactory) {
        this.relationshipEntityStateFactory = relationshipEntityStateFactory;
    }

    public EntityStateHandler getEntityStateHandler() {
        return entityStateHandler;
    }

    public TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public void setNodeTypeRepresentationStrategy(TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy) {
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
    }

    public TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    public void setRelationshipTypeRepresentationStrategy(TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy) {
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
    }

    public ConversionService getConversionService() {
        return conversionService;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validatorFactory) {
        this.validator = validatorFactory;
    }

    public void setMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }


    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    public void setGraphDatabase(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    public GraphDatabase getGraphDatabase() {
        return graphDatabase;
    }

    public ResultConverter getResultConverter() {
        return resultConverter;
    }

    public EntityRemover getEntityRemover() {
        return entityRemover;
    }

    public IndexProvider getIndexProvider() {
        return indexProvider;
    }

    public Neo4jEntityPersister getEntityPersister() {
        return entityPersister;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public TypeRepresentationStrategies getTypeRepresentationStrategies() {
        return typeRepresentationStrategies;
    }


    public CypherQueryExecutor getCypherQueryExecutor() {
        return cypherQueryExecutor;
    }

    public Neo4jMappingContext getMappingContext() {
        return mappingContext;
    }

    public void setTypeRepresentationStrategyFactory(TypeRepresentationStrategyFactory typeRepresentationStrategyFactory) {
        this.typeRepresentationStrategyFactory = typeRepresentationStrategyFactory;
    }
}
