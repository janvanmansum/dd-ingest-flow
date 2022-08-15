/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.dd2d.fieldbuilders

import nl.knaw.dans.easy.dd2d.mapping.JsonObject
import nl.knaw.dans.ingest.core.legacy.MapperForJava
import nl.knaw.dans.lib.dataverse.model.dataset.{ CompoundField, PrimitiveSingleValueField, SingleValueField }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.scaladv.serializeAsJson

import scala.collection.JavaConverters.{ mapAsJavaMapConverter, seqAsJavaListConverter }
import scala.collection.mutable

class CompoundFieldBuilder(name: String, multipleValues: Boolean = true) extends AbstractFieldBuilder with DebugEnhancedLogging{
  private val values = new mutable.ListBuffer[Map[String, SingleValueField]]

  def addValue(v: JsonObject): Unit = {
    if (!multipleValues && values.nonEmpty) throw new IllegalArgumentException("Trying to add a second value to a single value field")
    values.append(v.mapValues{v => // TODO poor error handling but only required until scala lib is fully eliminated
      val json = serializeAsJson(v).get
      MapperForJava.get.readValue(json, classOf[PrimitiveSingleValueField])
    })
  }

  override def build(deduplicate: Boolean = false): Option[CompoundField] = {
    if (values.nonEmpty) Option {
      val stringToFields = if (deduplicate) values.toList.distinct
                           else values.toList
      new CompoundField(
        name,
        multipleValues,
        stringToFields.map(_.asJava).asJava
      )
    }
    else Option.empty
  }
}
