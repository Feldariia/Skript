/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011-2013 Peter Güttinger
 * 
 */

package ch.njol.skript.expressions;

import java.lang.reflect.Array;

import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTargetEvent;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.CollectionUtils;
import ch.njol.util.Kleenean;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("serial")
@Name("Target")
@Description("For players this is the entity at the crosshair, while for mobs and experience orbs it represents the entity they are attacking/following (if any).")
@Examples({"on entity target:",
		"    entity's target is a player",
		"    send \"You're being followed by an %entity%!\" to target of entity"})
@Since("")
public class ExprTarget extends PropertyExpression<LivingEntity, Entity> {
	static {
		Skript.registerExpression(ExprTarget.class, Entity.class, ExpressionType.PROPERTY,
				"[the] target[[ed] %-*entitydata%] [of %livingentities%]",
				"%livingentities%'[s] target[[ed] %-*entitydata%]");
	}
	
	private EntityData<?> type;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		type = exprs[matchedPattern] == null ? null : (EntityData<?>) exprs[matchedPattern].getSingle(null);
		setExpr((Expression<? extends LivingEntity>) exprs[1 - matchedPattern]);
		return true;
	}
	
	@Override
	protected Entity[] get(final Event e, final LivingEntity[] source) {
		if (getTime() >= 0 && e instanceof EntityTargetEvent && getExpr().isDefault() && !Delay.isDelayed(e)) {
			final Entity en = ((EntityTargetEvent) e).getTarget();
			if (en != null) { // untarget event
				if (type != null && !type.isInstance(en)) {
					return (Entity[]) Array.newInstance(getReturnType(), 0);
				}
				final Entity[] r = (Entity[]) Array.newInstance(getReturnType(), 1);
				r[0] = en;
				return r;
			}
		}
		return get(source, new Converter<LivingEntity, Entity>() {
			@Override
			public Entity convert(final LivingEntity en) {
				return Utils.getTarget(en, type);
			}
		});
	}
	
	@Override
	public Class<? extends Entity> getReturnType() {
		return type == null ? Entity.class : type.getType();
	}
	
	@Override
	public String toString(final Event e, final boolean debug) {
		if (e == null)
			return "the target" + (type == null ? "" : "ed " + type) + " of " + getExpr().toString(e, debug);
		return Classes.getDebugMessage(getAll(e));
	}
	
	@Override
	public boolean setTime(final int time) {
		return super.setTime(time, EntityTargetEvent.class, getExpr());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.DELETE)
			return CollectionUtils.array(LivingEntity.class);
		return null;
	}
	
	@Override
	public void change(final Event e, final Object delta, final ChangeMode mode) {
		if (getTime() >= 0 && e instanceof EntityTargetEvent && getExpr().isDefault() && !Delay.isDelayed(e)) {
			((EntityTargetEvent) e).setTarget((LivingEntity) delta);
			return;
		}
		final LivingEntity target = (LivingEntity) delta;
		for (final LivingEntity entity : getExpr().getArray(e)) {
			if (!(entity instanceof Creature))
				continue;
			((Creature) entity).setTarget(target);
		}
	}
	
}