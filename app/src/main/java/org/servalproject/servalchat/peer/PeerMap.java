package org.servalproject.servalchat.peer;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Interface;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servaldna.Subscriber;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.factor;

/**
 * Created by jeremy on 1/05/17.
 */

public class PeerMap extends View implements INavigate, ILifecycle{
	private final Serval serval;
	private int gen=-1;
	private List<DrawNode> drawNodes = new ArrayList<>();
	private Map<SubscriberId, DrawPeer> peerMap = new HashMap<>();
	private Camera camera;
	private Paint circlePaint;
	private Paint linePaint;
	private Paint labelPaint;
	private Rect graphBounds;
	private static final String TAG = "PeerMap";

	public PeerMap(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		this.serval = Serval.getInstance();
		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setAntiAlias(true);

		linePaint = new Paint(Color.GRAY);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setAntiAlias(true);

		labelPaint = new Paint(Color.LTGRAY);
		labelPaint.setTextAlign(Paint.Align.LEFT);
		labelPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		labelPaint.setUnderlineText(false);
		labelPaint.setSubpixelText(true);
		labelPaint.setAntiAlias(true);

		camera = new Camera();
		Self.angle = 0;
		Self.arc = (float) (Math.PI * 2);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		recalcFinalPositions();
		moveNodes();

		float xScale = (float)canvas.getWidth() / graphBounds.width();
		float yScale = (float)canvas.getHeight() / graphBounds.height();
		float scale = ((xScale > yScale) ? yScale : xScale) * 0.95f;
		if (scale > 2)
			scale = 2;

		canvas.save();
		canvas.translate(canvas.getWidth()/2 - graphBounds.centerX()*scale, canvas.getHeight()/2 - graphBounds.centerY()*scale);
		canvas.scale(scale, scale);
		for(DrawNode node : drawNodes) {
			if (node.visible())
				node.draw(canvas);
		}
		canvas.restore();
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Bundle args) {
		return this;
	}

	@Override
	public void onDetach(boolean configChange) {

	}

	private void add(Peer peer){
		DrawPeer draw = new DrawPeer(peer);
		drawNodes.add(draw);
		peerMap.put(peer.getSubscriber().sid, draw);
	}

	@Override
	public void onVisible() {
		int g = serval.knownPeers.peerListObservers.add(peerObserver);
		if (g!=gen){
			drawNodes.clear();
			drawNodes.add(Self);
			for(Peer p : serval.knownPeers.getReachablePeers())
				add(p);
			gen = g;
		}
		finalPositionsDirty = true;
		invalidate();
	}

	@Override
	public void onHidden() {
		gen = serval.knownPeers.peerListObservers.remove(peerObserver);
	}

	private ListObserver<Peer> peerObserver = new ListObserver<Peer>() {
		@Override
		public void added(Peer peer) {
			add(peer);
			PeerMap.this.invalidate();
		}

		@Override
		public void removed(Peer obj) {

		}

		@Override
		public void updated(Peer peer) {
			finalPositionsDirty = true;
			PeerMap.this.invalidate();
			//DrawPeer draw = peerMap.get(peer);
			//draw.update();
		}

		@Override
		public void reset() {

		}
	};


	private boolean finalPositionsDirty = true;

	private void recalcFinalPositions(int first, int last) {
		// fan out these nodes in an arc on a circle
		DrawNode firstNode = drawNodes.get(first);
		DrawNode parent = firstNode.getParent();
		if (parent == null)
			return;
		double baseArc = parent.arc;
		double baseAngle = parent.angle - parent.arc/2;
		int count = last - first;
		String parentLabel = parent.getLabel();
		int distance = firstNode.getDistance();
		double radius = distance * 100;
		double childSpacing = baseArc / count;
		double childArc = childSpacing;
		if (distance <4 && childArc > Math.PI*2 / Math.pow(2, distance))
			childArc = Math.PI*2 / Math.pow(2, distance);
		baseAngle += childSpacing/2;
		Log.v(TAG, parentLabel + " has "+count+" children from "+baseAngle+" + N * "+childSpacing);

		for (int i = first; i < last; i++) {
			DrawNode node = drawNodes.get(i);
			node.arc = childArc;
			node.angle = baseAngle + ((i - first) * childSpacing);
			Log.v(TAG, node.getLabel()+" " + i + " ("+(i - first)+" of " + count + ") angle " + node.angle);
			node.destX = (float) (Math.sin(node.angle) * radius);
			node.destY = (float) (Math.cos(node.angle) * radius);
		}
	}

	private void recalcFinalPositions(){
		if (!finalPositionsDirty)
			return;

		Collections.sort(drawNodes);
		Self.x = 0;
		Self.y = 0;
		Self.angle = 0;
		Self.arc = (float) (Math.PI * 2);

		if (drawNodes.size() > 1){
			int start = 1;
			int index = start+1;

			DrawNode first = drawNodes.get(start);

			for (; index < drawNodes.size(); index++){
				DrawNode node = drawNodes.get(index);
				if (node.getDistance() != first.getDistance() ||
						node.getParent() != first.getParent()) {
					recalcFinalPositions(start, index);
					start = index;
					first = node;
				}
			}

			if (start!=index)
				recalcFinalPositions(start, index);

		}

		finalPositionsDirty = false;
	}

	private void moveNodes(){
		// TODO animate each node to move towards it's destx/desty

		Rect bounds = new Rect();

		for(DrawNode n : drawNodes){
			if (n.visible()) {
				n.x = n.destX;
				n.y = n.destY;
				n.calcBounds();
				bounds.union(n.bounds);
			}
		}
		graphBounds = bounds;
	}

	private abstract class DrawNode implements Comparable<DrawNode>{
		float destX;
		float destY;
		float x;
		float y;
		Rect bounds;

		double angle;
		double arc;

		abstract int getDistance();
		abstract int getId();
		abstract String getLabel();
		abstract int getColor();
		abstract boolean visible();

		void calcBounds(){
			bounds = new Rect();
			String label = getLabel();
			if (label!=null && !"".equals(label)) {
				labelPaint.getTextBounds(label, 0, label.length(), bounds);
				bounds.offset(- bounds.width()/2, 12 + bounds.height());
			}
			bounds.union(-10,-10,10,10);
			bounds.offset((int)x, (int)y);
		}
		void draw(Canvas canvas) {
			circlePaint.setColor(getColor());
			canvas.drawCircle(this.x, this.y, 10, circlePaint);
			DrawNode parent = getParent();
			if (parent != null)
				// see https://issuetracker.google.com/issues/36980542
				canvas.drawLines(new float[]{
						-100000f, -100000f, -100000f, -100000f,
						this.x, this.y, parent.x, parent.y}, linePaint);
			String label = getLabel();
			if (label!=null && !"".equals(label)){
				Rect dest =  new Rect();
				labelPaint.getTextBounds(label, 0, label.length(), dest);
				canvas.drawText(label, 0, label.length(),
						this.x - dest.width()/2, this.y + 12 + dest.height(), labelPaint);
			}
		}

		abstract DrawNode getParent();

		@Override
		public int compareTo(@NonNull DrawNode drawNode) {
			if (this == drawNode)
				return 0;

			int distance = getDistance();
			int theirDistance = drawNode.getDistance();
			if (distance != theirDistance)
				return (distance < theirDistance) ? -1 : 1;

			DrawNode myParent = getParent();
			DrawNode theirParent = drawNode.getParent();
			if (myParent != theirParent){
				if (myParent==null)
					return -1;
				if (theirParent==null)
					return 1;
				return myParent.compareTo(theirParent);
			}

			return getId() < drawNode.getId() ? -1 : 1;
		}
	}

	private DrawNode Self = new DrawNode() {
		@Override
		int getDistance() {
			return 0;
		}

		@Override
		int getId() {
			return 0;
		}

		@Override
		String getLabel() {
			return "Self";
		}

		@Override
		int getColor() {
			return Color.BLACK;
		}

		@Override
		boolean visible() {
			return true;
		}

		@Override
		DrawNode getParent() {
			return null;
		}
	};

	private class DrawPeer extends DrawNode{
		final Peer peer;
		private int hopCount;

		DrawPeer(Peer peer){
			this.peer = peer;
		}

		@Override
		int getDistance() {
			int newHopCount = peer.getHopCount();
			if (newHopCount >=1 )
				hopCount = newHopCount;
			return hopCount;
		}

		@Override
		int getId() {
			return (int) peer.getId();
		}

		@Override
		String getLabel() {
			return peer.displayName();
		}

		@Override
		int getColor() {
			Interface netInterface = peer.getNetInterface();
			if (netInterface!=null){
				if (netInterface.name.equals("bluetooth"))
					return Color.BLUE;
				return Color.GREEN;
			}
			return Color.LTGRAY;
		}

		@Override
		boolean visible() {
			return peer.getHopCount()>=1;
		}

		@Override
		DrawNode getParent() {
			Peer priorHop = peer.getPriorHop();
			if (priorHop==null)
				return Self;
			return peerMap.get(priorHop.getSubscriber().sid);
		}
	}
}
