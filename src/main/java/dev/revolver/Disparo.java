package dev.revolver;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** O tiro das duas armas, no servidor. */
public final class Disparo {
    public static final double ALCANCE = 64.0;

    private Disparo() {}

    /** O tiro: um raio do olho até 64 blocos; acerta a primeira criatura ou o primeiro bloco no caminho. */
    public static void atirar(ServerLevel level, Player jogador, boolean outraMao, float dano) {
        Vec3 olho = jogador.getEyePosition();
        Vec3 dir = jogador.getLookAngle();
        Vec3 fim = olho.add(dir.scale(ALCANCE));
        BlockHitResult bloco = level.clip(new ClipContext(olho, fim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, jogador));
        Vec3 fimReal = bloco.getType() == HitResult.Type.MISS ? fim : bloco.getLocation();
        AABB caixa = jogador.getBoundingBox().expandTowards(fimReal.subtract(olho)).inflate(1.0);
        EntityHitResult alvo = ProjectileUtil.getEntityHitResult(level, jogador, olho, fimReal, caixa,
                e -> e != jogador && e.isPickable() && !e.isSpectator());

        // fumaça na boca do cano (um pouco à direita e abaixo do olho, onde a mão segura)
        Vec3 direita = dir.cross(new Vec3(0, 1, 0)).normalize();
        if (outraMao) direita = direita.scale(-1);
        Vec3 boca = olho.add(dir.scale(0.9)).add(direita.scale(0.28)).add(0, -0.18, 0);
        level.sendParticles(ParticleTypes.SMOKE, boca.x, boca.y, boca.z, 8, 0.03, 0.03, 0.03, 0.02);
        level.sendParticles(ParticleTypes.POOF, boca.x, boca.y, boca.z, 2, 0.02, 0.02, 0.02, 0.01);

        if (alvo != null) {
            Entity atingido = alvo.getEntity();
            atingido.hurt(level.damageSources().playerAttack(jogador), dano);
            if (atingido instanceof LivingEntity vivo) vivo.knockback(0.3, -dir.x, -dir.z);
            Vec3 p = alvo.getLocation();
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 10, 0.1, 0.1, 0.1, 0.2);
        } else if (bloco.getType() == HitResult.Type.BLOCK) {
            BlockState estadoBloco = level.getBlockState(bloco.getBlockPos());
            Vec3 p = bloco.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estadoBloco), p.x, p.y, p.z, 12,
                    0.05, 0.05, 0.05, 0.1);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 6, 0.02, 0.02, 0.02, 0.25);
            level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 3, 0.02, 0.02, 0.02, 0.01);
        }
    }
}
