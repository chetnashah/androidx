/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.impl.event.MeasureCallbackEvent
import androidx.health.services.client.impl.ipc.internal.ListenerKey
import androidx.health.services.client.impl.response.DataPointsResponse
import androidx.health.services.client.proto.EventsProto
import androidx.health.services.client.proto.EventsProto.MeasureCallbackEvent.EventCase
import com.google.common.util.concurrent.MoreExecutors
import java.util.HashMap
import java.util.concurrent.Executor

/**
 * A stub implementation for IMeasureCallback.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MeasureCallbackStub
private constructor(callbackKey: MeasureCallbackKey, private val callback: MeasureCallback) :
    IMeasureCallback.Stub() {

    public val listenerKey: ListenerKey = ListenerKey(callbackKey)

    @get:VisibleForTesting
    public var executor: Executor = MoreExecutors.directExecutor()
        private set

    override fun onMeasureCallbackEvent(event: MeasureCallbackEvent) {
        executor.execute { triggerListener(event.proto) }
    }

    private fun triggerListener(proto: EventsProto.MeasureCallbackEvent) {
        when (proto.eventCase) {
            EventCase.DATA_POINT_RESPONSE -> {
                val dataPointsResponse = DataPointsResponse(proto.dataPointResponse)
                callback.onDataReceived(dataPointsResponse.dataPoints)
            }
            EventCase.AVAILABILITY_RESPONSE ->
                callback.onAvailabilityChanged(
                    DataType(proto.availabilityResponse.dataType),
                    Availability.fromProto(proto.availabilityResponse.availability)
                )
            null, EventCase.EVENT_NOT_SET -> Log.w(TAG, "Received unknown event ${proto.eventCase}")
        }
    }

    /**
     * Its important to use the same stub for registration and un-registration, to ensure same
     * binder object is passed by framework to service side of the IPC.
     */
    public class MeasureCallbackCache private constructor() {
        @GuardedBy("this")
        private val listeners: MutableMap<MeasureCallbackKey, MeasureCallbackStub> = HashMap()

        @Synchronized
        public fun getOrCreate(
            dataType: DataType,
            measureCallback: MeasureCallback,
            executor: Executor
        ): MeasureCallbackStub {
            val callbackKey = MeasureCallbackKey(dataType, measureCallback)

            // If a measure callback happens for the same datatype with same callback, pass the same
            // stub instance, but update the executor, as executor might have changed. Its ok to
            // register
            // the callback once again, as on our service implementation re-registering for same
            // datatype
            // with  same callback is a no-op.
            var measureCallbackStub = listeners[callbackKey]
            if (measureCallbackStub == null) {
                measureCallbackStub = MeasureCallbackStub(callbackKey, measureCallback)
                listeners[callbackKey] = measureCallbackStub
            }
            measureCallbackStub.executor = executor
            return measureCallbackStub
        }

        @Synchronized
        public fun remove(
            dataType: DataType,
            measureCallback: MeasureCallback
        ): MeasureCallbackStub? {
            val callbackKey = MeasureCallbackKey(dataType, measureCallback)
            return listeners.remove(callbackKey)
        }

        public companion object {
            @JvmField public val INSTANCE: MeasureCallbackCache = MeasureCallbackCache()
        }
    }

    private data class MeasureCallbackKey(
        private val dataType: DataType,
        private val measureCallback: MeasureCallback
    )

    private companion object {
        const val TAG = "MeasureCallbackStub"
    }
}
