package com.nttdata.bootcamp.controller;

import com.nttdata.bootcamp.entity.Withdrawal;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nttdata.bootcamp.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import javax.validation.Valid;

@RestController
@RequestMapping(value = "/withdrawal")
public class WithdrawalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(WithdrawalController.class);
	@Autowired
	private WithdrawalService withdrawalService;

	//withdrawal search
	@GetMapping("/findAllWithdrawal")
	public Flux<Withdrawal> findAllWithdrawal() {
		Flux<Withdrawal> deposits = withdrawalService.findAll();
		LOGGER.info("Registered withdrawal: " + deposits);
		return deposits;
	}

	//withdrawal by AccountNumber
	@GetMapping("/findAllWithdrawalByAccountNumber/{accountNumber}")
	public Flux<Withdrawal> findAllWithdrawalByAccountNumber(@PathVariable("accountNumber") String accountNumber) {
		Flux<Withdrawal> withdrawalFlux = withdrawalService.findByAccountNumber(accountNumber);
		LOGGER.info("Registered withdrawal of account number: "+accountNumber +"-" + withdrawalFlux);
		return withdrawalFlux;
	}

	//withdrawal  by Number
	@CircuitBreaker(name = "withdrawal", fallbackMethod = "fallBackGetWithdrawal")
	@GetMapping("/findByWithdrawalNumber/{numberDeposits}")
	public Mono<Withdrawal> findByWithdrawalNumber(@PathVariable("numberDeposits") String numberDeposits) {
		LOGGER.info("Searching withdrawal by number: " + numberDeposits);
		return withdrawalService.findByNumber(numberDeposits);
	}

	//Save withdrawal
	@CircuitBreaker(name = "withdrawal", fallbackMethod = "fallBackGetWithdrawal")
	@PostMapping(value = "/saveWithdrawal/{commission}/{count}")
	public Mono<Withdrawal> saveWithdrawal(@RequestBody Withdrawal dataWithdrawal,
										   @PathVariable("commission") Double commission,
										   @PathVariable("count") Long count){
		Mono<Long> countMovementsMono = getCountDeposits(dataWithdrawal.getAccountNumber());
		Long countMovementS =countMovementsMono.block();

		Mono.just(dataWithdrawal).doOnNext(t -> {
					if(countMovementS>count)
						t.setCommission(commission);
					else
						t.setCommission(new Double("0.00"));
					t.setTypeAccount("passive");
					t.setCreationDate(new Date());
					t.setModificationDate(new Date());


				}).onErrorReturn(dataWithdrawal).onErrorResume(e -> Mono.just(dataWithdrawal))
				.onErrorMap(f -> new InterruptedException(f.getMessage())).subscribe(x -> LOGGER.info(x.toString()));

		Mono<Withdrawal> withdrawalMono = withdrawalService.saveWithdrawal(dataWithdrawal);
		return withdrawalMono;
	}

	//Update withdrawal
	@CircuitBreaker(name = "withdrawal", fallbackMethod = "fallBackGetWithdrawal")
	@PutMapping("/updateWithdrawal/{numberTransaction}")
	public Mono<Withdrawal> updateWithdrawal(@RequestBody Withdrawal dataWithdrawal,@PathVariable("numberTransaction") String numberTransaction) {
		Mono.just(dataWithdrawal).doOnNext(t -> {

					t.setWithdrawalNumber(numberTransaction);
					t.setModificationDate(new Date());

				}).onErrorReturn(dataWithdrawal).onErrorResume(e -> Mono.just(dataWithdrawal))
				.onErrorMap(f -> new InterruptedException(f.getMessage())).subscribe(x -> LOGGER.info(x.toString()));

		Mono<Withdrawal> withdrawalMono = withdrawalService.updateWithdrawal(dataWithdrawal);
		return withdrawalMono;
	}


	//Delete withdrawal
	@CircuitBreaker(name = "withdrawal", fallbackMethod = "fallBackGetWithdrawal")
	@DeleteMapping("/deleteWithdrawal/{numberTransaction}")
	public Mono<Void> deleteWithdrawal(@PathVariable("numberTransaction") String numberTransaction) {
		LOGGER.info("Deleting withdrawal by number: " + numberTransaction);
		Mono<Void> delete = withdrawalService.deleteWithdrawal(numberTransaction);
		return delete;

	}


	@GetMapping("/getCountDeposits/{accountNumber}")
	//get count of withdrawal
	public Mono<Long> getCountDeposits(@PathVariable("accountNumber") String accountNumber){
		Flux<Withdrawal> transactions= findAllWithdrawalByAccountNumber(accountNumber);
		return transactions.count();
	}


	private Mono<Withdrawal> fallBackGetWithdrawal(Exception e){
		Withdrawal withdrawal = new Withdrawal();
		Mono<Withdrawal> withdrawalMono= Mono.just(withdrawal);
		return withdrawalMono;
	}

	@GetMapping("/getCommissionsDeposit/{accountNumber}")
	public Flux<Withdrawal> getCommissionsDeposit(@PathVariable("accountNumber") String accountNumber) {
		Flux<Withdrawal> commissions = withdrawalService.findByCommission(accountNumber);
		LOGGER.info("Registered commissions withdrawal of account number: "+accountNumber +"-" + commissions);
		return commissions;
	}


}
