package com.whz.portfolio.infrastructure.persistence;

import com.whz.portfolio.infrastructure.EventTypes;
import com.whz.portfolio.infrastructure.PortfolioData;
import com.whz.portfolio.model.portfolio.PortfolioCreated;
import io.vlingo.lattice.model.projection.Projectable;
import io.vlingo.lattice.model.projection.StateStoreProjectionActor;
import io.vlingo.symbio.Source;

public class PortfolioProjectionActor extends StateStoreProjectionActor<PortfolioData> {
	private static final PortfolioData Empty = PortfolioData.empty();

	public PortfolioProjectionActor() {
		super(QueryModelStateStoreProvider.instance().store);
	}

	@Override
	protected PortfolioData currentDataFor(final Projectable projectable) {
		return Empty;
	}

	@Override
	protected PortfolioData merge(final PortfolioData previousData,
								  final int previousVersion,
								  final PortfolioData currentData,
								  final int currentVersion) {
		PortfolioData merged = null;

		for (final Source<?> event : sources()) {
			switch (EventTypes.valueOf(event.typeName())) {
			case PortfolioCreated:
				final PortfolioCreated portfolioCreated = typed(event);
				merged = PortfolioData.from(portfolioCreated.id, portfolioCreated.owner); // fixed and closed
				break;
			default:
				merged = Empty;
				logger().warn("Event of type " + event.typeName() + " was not matched.");
				break;
			}
		}

		return merged;
	}

}
