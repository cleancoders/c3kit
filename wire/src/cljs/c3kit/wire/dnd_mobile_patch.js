goog.provide('c3kit.wire.dnd_mobile_patch');

goog.require('goog.fx.AbstractDragDrop');
goog.require('goog.fx.DragDropGroup');

// MDM - This Patch is based on a commit to Closure that was later reverted.
// https://git.codepku.com/zmcdbp/closure-library/commit/b996453ea82014ceaf004d41aed5d0f4dc3f07aa

goog.fx.AbstractDragDrop.prototype.initItem = function (item) {
    if (this.isSource_) {
        // goog.events.listen(
        //     item.element,
        //     [goog.events.EventType.MOUSEDOWN, goog.events.EventType.TOUCHSTART],
        //     item.mouseDown_, false, item);
        goog.events.listen(item.element, goog.events.EventType.MOUSEDOWN, item.mouseDown_, false, item);
        goog.events.listen(item.element, goog.events.EventType.TOUCHSTART, item.touchStart_, false, item);
        goog.events.listen(item.element, goog.events.EventType.TOUCHEND, item.touchEnd_, false, item);

        if (this.sourceClass_) {
            goog.dom.classlist.add(
                goog.asserts.assert(item.element), this.sourceClass_);
        }
    }

    if (this.isTarget_ && this.targetClass_) {
        goog.dom.classlist.add(
            goog.asserts.assert(item.element), this.targetClass_);
    }
};

goog.fx.AbstractDragDrop.prototype.disposeItem = function (item) {
    if (this.isSource_) {
        goog.events.unlisten(item.element, goog.events.EventType.MOUSEDOWN, item.mouseDown_, false, item);
        goog.events.unlisten(item.element, goog.events.EventType.TOUCHSTART, item.touchStart_, false, item);
        goog.events.unlisten(item.element, goog.events.EventType.TOUCHEND, item.touchEnd_, false, item);

        if (this.sourceClass_) {
            goog.dom.classlist.remove(
                goog.asserts.assert(item.element), this.sourceClass_);
        }
    }
    if (this.isTarget_ && this.targetClass_) {
        goog.dom.classlist.remove(
            goog.asserts.assert(item.element), this.targetClass_);
    }
    item.dispose();
};

goog.fx.DragDropItem.prototype.mouseDown_ = function (event) {
    if (!event.isMouseActionButton()) {
        return;
    }

    // Get the draggable element for the target.
    var element = this.getDraggableElement(/** @type {Element} */ (event.target));
    if (element) {
        this.maybeStartDrag_(event, element);
    }
};

goog.fx.DragDropItem.prototype.maybeStartDrag_ = function (event, element) {
    var eventType = goog.events.EventType;
    this.eventHandler_
        .listen(element, eventType.MOUSEMOVE, this.mouseMove_, false)
        .listen(element, eventType.MOUSEOUT, this.mouseMove_, false)
        .listen(element, eventType.TOUCHMOVE, this.mouseMove_, false);


    // Capture the MOUSEUP on the document to ensure that we cancel the start
    // drag handlers even if the mouse up occurs on some other element. This can
    // happen for instance when the mouse down changes the geometry of the element
    // clicked on (e.g. through changes in activation styling) such that the mouse
    // up occurs outside the original element.
    var doc = goog.dom.getOwnerDocument(element);
    this.eventHandler_.listen(doc, eventType.MOUSEUP, this.mouseUp_, true);
    // this.eventHandler_.listen(doc, eventType.MOUSEUP, this.mouseUp_, true).listen(doc, eventType.TOUCHEND, this.mouseUp_, true);

    // this.currentDragElement_ = element;
    // MDM - What we want is the element that we registered for drag-n-drop, not any child element.
    this.currentDragElement_ = this.element;

    this.startPosition_ = new goog.math.Coordinate(event.clientX, event.clientY);
};

goog.fx.DragDropItem.prototype.touchStart_ = function (event) {
    // Get the draggable element for the target.
    var element = this.getDraggableElement(/** @type {Element} */ (event.target));
    if (element) {
        this.maybeStartTouchDrag_(event, element);
    }
};

goog.fx.DragDropItem.prototype.maybeStartTouchDrag_ = function (event, element) {
    var currentItem = this;
    this.touchTimer = setTimeout(function(){
            currentItem.startTouchDrag(event); },
        400);

    var doc = goog.dom.getOwnerDocument(element);
    this.eventHandler_.listen(doc,goog.events.EventType.TOUCHEND, this.touchEnd_, true);

    // this.currentDragElement_ = element;
    // MDM - What we want is the element that we registered for drag-n-drop, not any child element.
    this.currentDragElement_ = this.element;
    this.startPosition_ = new goog.math.Coordinate(event.clientX, event.clientY);
};

goog.fx.DragDropItem.prototype.startTouchDrag = function (event) {
    this.eventHandler_.removeAll();
    this.parent_.startDrag(event, this);
}

goog.fx.DragDropItem.prototype.touchEnd_ = function (event) {
    if(this.touchTimer) {
        clearTimeout(this.touchTimer);
    }
};

