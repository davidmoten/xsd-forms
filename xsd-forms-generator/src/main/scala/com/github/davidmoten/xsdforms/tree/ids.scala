package com.github.davidmoten.xsdforms.tree

private[xsdforms] object Ids {
  
  import com.github.davidmoten.xsdforms.{Prefix}

  val InstanceDelimiter = "-instance-"
  val ChoiceIndexDelimiter = "-choice-"

  def getItemId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-" + number + InstanceDelimiter + instances

  def getItemName(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-input-" + number + InstanceDelimiter + instances;

  def getItemErrorId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-error-" + number + InstanceDelimiter + instances

  def getChoiceItemId(number: Int, index: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    getItemId(number, instances)(idPrefix) + ChoiceIndexDelimiter + index

  def getChoiceItemName(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-input-" + number + InstanceDelimiter + instances

  def choiceContentId(number: Int, index: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "choice-content-" + number + InstanceDelimiter +
      instances + ChoiceIndexDelimiter + index

  def getItemId(number: Int, enumeration: Integer,
    instances: Instances)(implicit idPrefix: Prefix): String =
    getItemId(number, instances) + "-" + enumeration

  def getRepeatButtonId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "repeat-button-" + number + InstanceDelimiter + instances

  def getRemoveButtonId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "remove-button-" + number + InstanceDelimiter + instances

  def getRepeatingEnclosingId(number: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "repeating-enclosing-" + number + InstanceDelimiter + instances

  def getMinOccursZeroId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "min-occurs-zero-" + number + InstanceDelimiter + instances

  def getMinOccursZeroName(number: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "min-occurs-zero-name" + number + InstanceDelimiter + instances

  def getItemEnclosingId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-enclosing-" + number + InstanceDelimiter + instances

  def getPathId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-path-" + number + InstanceDelimiter + instances
}