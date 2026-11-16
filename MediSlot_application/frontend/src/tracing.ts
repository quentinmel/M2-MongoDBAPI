import {OTLPTraceExporter} from '@opentelemetry/exporter-trace-otlp-http';
import {ZoneContextManager} from '@opentelemetry/context-zone';
import {W3CTraceContextPropagator} from '@opentelemetry/core';
import {registerInstrumentations} from '@opentelemetry/instrumentation';
import {FetchInstrumentation} from '@opentelemetry/instrumentation-fetch';
import {resourceFromAttributes} from '@opentelemetry/resources';
import {BatchSpanProcessor} from '@opentelemetry/sdk-trace-base';
import {WebTracerProvider} from '@opentelemetry/sdk-trace-web';
import 'zone.js';

let demarre = false;

export function demarrerTraces(): void {
  if (demarre) return;
  demarre = true;

  const provider = new WebTracerProvider({
    resource: resourceFromAttributes({
      'service.name': 'medislot-web',
      'service.version': '1.0.0',
    }),
    spanProcessors: [
      new BatchSpanProcessor(new OTLPTraceExporter({
        url: `${window.location.origin}/otlp/v1/traces`,
      }), {
        scheduledDelayMillis: 1000,
      }),
    ],
  });

  provider.register({
    contextManager: new ZoneContextManager(),
    propagator: new W3CTraceContextPropagator(),
  });

  registerInstrumentations({
    instrumentations: [
      new FetchInstrumentation({
        ignoreUrls: [
          /\/otlp\/v1\/traces(?:\?|$)/,
          /\/api\/professionals(?:\?|$)/,
        ],
        propagateTraceHeaderCorsUrls: [/\/api(?:\/|$)/],
      }),
    ],
  });
}
