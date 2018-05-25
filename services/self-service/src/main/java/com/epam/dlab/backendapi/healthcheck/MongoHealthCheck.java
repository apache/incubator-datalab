package com.epam.dlab.backendapi.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.epam.dlab.mongo.MongoService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongoHealthCheck extends HealthCheck {
	private final MongoService mongoService;

	@Inject
	public MongoHealthCheck(MongoService mongoService) {
		this.mongoService = mongoService;
	}

	@Override
	protected Result check() {
		try {
			mongoService.ping();
		} catch (Exception e) {
			log.error("Mongo is unavailable {}", e.getMessage());
			return Result.unhealthy(e.getMessage());
		}
		return Result.healthy();
	}
}
