package com.whz.stockquote.infrastructure;

import com.whz.stockquote.infrastructure.persistence.QueryModelStateStoreProvider;
import com.whz.stockquote.infrastructure.persistence.ProjectionDispatcherProvider;
import io.vlingo.lattice.model.sourcing.SourcedTypeRegistry;
import com.whz.stockquote.resource.StockQuoteResource;
import io.vlingo.lattice.model.stateful.StatefulTypeRegistry;
import com.whz.stockquote.infrastructure.persistence.CommandModelJournalProvider;

import io.vlingo.actors.GridAddressFactory;
import io.vlingo.actors.Stage;
import io.vlingo.actors.World;
import io.vlingo.common.identity.IdentityGeneratorType;
import io.vlingo.http.resource.Configuration.Sizing;
import io.vlingo.http.resource.Configuration.Timing;
import io.vlingo.http.resource.Resources;
import io.vlingo.http.resource.Server;

public class Bootstrap {
    private static Bootstrap instance;
    private static int DefaultPort = 18083;

    private final Server server;
    private final World world;

    public Bootstrap(final int port) throws Exception {
        world = World.startWithDefaults("stockquote");

        final Stage stage =
                world.stageNamed("stockquote", Stage.class, new GridAddressFactory(IdentityGeneratorType.RANDOM));

        final SourcedTypeRegistry sourcedTypeRegistry = new SourcedTypeRegistry(world);
        final StatefulTypeRegistry statefulTypeRegistry = new StatefulTypeRegistry(world);
        QueryModelStateStoreProvider.using(stage, statefulTypeRegistry);
        CommandModelJournalProvider.using(stage, sourcedTypeRegistry, ProjectionDispatcherProvider.using(stage).storeDispatcher);

        final StockQuoteResource stockQuoteResource = new StockQuoteResource(stage);

        Resources allResources = Resources.are(
                stockQuoteResource.routes()
        );

        server = Server.startWith(stage, allResources, port, Sizing.define(), Timing.define());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.server.stop();

                System.out.println("\n");
                System.out.println("=========================");
                System.out.println("Stopping stockquote.");
                System.out.println("=========================");
            }
        }));
    }

    void stopServer() throws Exception {
        if (instance == null) {
            throw new IllegalStateException("Schemata server not running");
        }
        instance.server.stop();
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("=========================");
        System.out.println("service: stockquote.");
        System.out.println("=========================");

        int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            port = DefaultPort;
            System.out.println("stockquote: Command line does not provide a valid port; defaulting to: " + port);
        }

        instance = new Bootstrap(port);
    }
}